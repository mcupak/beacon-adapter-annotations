package com.dnastack.beacon.adater.annotations;

import com.dnastack.beacon.adapter.api.BeaconAdapter;
import com.dnastack.beacon.adater.annotations.client.ga4gh.Ga4ghClient;
import com.dnastack.beacon.adater.annotations.client.ga4gh.exceptions.Ga4ghClientException;
import com.dnastack.beacon.exceptions.BeaconAlleleRequestException;
import com.dnastack.beacon.exceptions.BeaconException;
import com.dnastack.beacon.utils.AdapterConfig;
import com.dnastack.beacon.utils.ConfigValue;
import com.dnastack.beacon.utils.Reason;
import com.google.common.collect.Iterables;
import ga4gh.AlleleAnnotations.VariantAnnotationSet;
import ga4gh.References.ReferenceSet;
import ga4gh.Variants;
import ga4gh.Variants.CallSet;
import ga4gh.Variants.Variant;
import ga4gh.Variants.VariantSet;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.ga4gh.beacon.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Exposes Ga4gh variant annotations as a Beacon.
 * Queries the underlying Ga4gh client and assembles Beacon responses.
 *
 * @author Artem (tema.voskoboynick@gmail.com)
 * @author Miro Cupak (mirocupak@gmail.com)
 * @version 1.0
 */
public class AnnotationsBeaconAdapter implements BeaconAdapter {

    public static final Beacon SAMPLE_BEACON = Beacon.newBuilder()
            .setId("sample-beacon")
            .setName("Sample Beacon")
            .setApiVersion("0.3.0")
            .setCreateDateTime("01.01.2016")
            .setUpdateDateTime("01.01.2016")
            .setOrganization(BeaconOrganization.newBuilder()
                    .setId("sample-organization")
                    .setName("Sample Organization")
                    .build())
            .build();

    private Ga4ghClient ga4ghClient = new Ga4ghClient();

    /**
     * Copy of the the Java 8 function, but can throw {@link BeaconAlleleRequestException}.
     */
    @FunctionalInterface
    public interface FunctionThrowingAlleleRequestException<T, R> {

        R apply(T t) throws BeaconAlleleRequestException;
    }

    private void initGa4ghClient(AdapterConfig adapterConfig) {
        String ga4ghBaseUrl = getGa4ghBaseUrl(adapterConfig);
        ga4ghClient = ga4ghBaseUrl == null ? new Ga4ghClient() : new Ga4ghClient(ga4ghBaseUrl);
    }

    private String getGa4ghBaseUrl(AdapterConfig adapterConfig) {
        List<ConfigValue> configValues = adapterConfig.getConfigValues();
        ConfigValue ga4ghBaseUrl = Iterables.getFirst(configValues, null);
        return ga4ghBaseUrl == null ? null : ga4ghBaseUrl.getValue();
    }

    private BeaconAlleleRequest createRequest(String referenceName,
                                              Long start,
                                              String referenceBases,
                                              String alternateBases,
                                              String assemblyId,
                                              List<String> datasetIds,
                                              Boolean includeDatasetResponses) {
        return BeaconAlleleRequest.newBuilder()
                .setReferenceName(referenceName)
                .setStart(start)
                .setReferenceBases(referenceBases)
                .setAlternateBases(alternateBases)
                .setAssemblyId(assemblyId)
                .setDatasetIds(datasetIds)
                .setIncludeDatasetResponses(includeDatasetResponses)
                .build();
    }

    private BeaconAlleleResponse doGetBeaconAlleleResponse(String referenceName,
                                                           Long start,
                                                           String referenceBases,
                                                           String alternateBases,
                                                           String assemblyId,
                                                           List<String> datasetIds,
                                                           Boolean includeDatasetResponses) throws BeaconException {
        List<BeaconDatasetAlleleResponse> datasetResponses = map(datasetIds,
                datasetId -> getDatasetResponse(datasetId,
                        assemblyId,
                        referenceName,
                        start,
                        referenceBases,
                        alternateBases));
        // Dataset response is null if no matching variant set could be found.
        datasetResponses.removeIf(Objects::isNull);

        List<BeaconDatasetAlleleResponse> returnedDatasetResponses = BooleanUtils.isTrue(includeDatasetResponses)
                ? datasetResponses
                : null;

        BeaconError anyError = datasetResponses.stream()
                .map(BeaconDatasetAlleleResponse::getError)
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);

        Boolean exists = anyError != null
                ? null
                : datasetResponses.stream().anyMatch(BeaconDatasetAlleleResponse::getExists);

        return BeaconAlleleResponse.newBuilder()
                .setAlleleRequest(null)
                .setDatasetAlleleResponses(returnedDatasetResponses)
                .setBeaconId(SAMPLE_BEACON.getId())
                .setError(anyError)
                .setExists(exists)
                .build();
    }

    private BeaconDatasetAlleleResponse getDatasetResponse(String datasetId,
                                                           String assemblyId,
                                                           String referenceName,
                                                           long start,
                                                           String referenceBases,
                                                           String alternateBases) throws BeaconAlleleRequestException {
        VariantSet variantSet = getVariantSetToSearch(datasetId, assemblyId);
        if (variantSet == null) {
            // No matching variant set.
            return null;
        }

        List<Variant> variants = loadVariants(variantSet.getId(), referenceName, start).stream()
                .filter(variant -> isVariantMatchesRequested(
                        variant,
                        referenceBases,
                        alternateBases))
                .collect(Collectors.toList());

        Double frequency = calculateFrequency(alternateBases, variants);

        Long sampleCount = countSamples(variants);

        Long callsCount = variants.stream().mapToLong(Variant::getCallsCount).sum();

        long variantCount = (long) variants.size();

        boolean exists = CollectionUtils.isNotEmpty(variants);

        return BeaconDatasetAlleleResponse.newBuilder()
                .setDatasetId(datasetId)
                .setFrequency(frequency)
                .setCallCount(callsCount)
                .setVariantCount(variantCount)
                .setSampleCount(sampleCount)
                .setExists(exists)
                .build();
    }

    private Long countSamples(List<Variant> variants) throws BeaconAlleleRequestException {
        Stream<String> callSetIds = variants.stream()
                .flatMap(variant -> variant.getCallsList().stream())
                .map(Variants.Call::getCallSetId);

        return map(callSetIds, this::loadCallSet).stream().map(CallSet::getBiosampleId).distinct().count();
    }

    private Double calculateFrequency(String alternateBases, List<Variant> variants) {
        Long matchingGenotypesCount = variants.stream()
                .mapToLong(variant -> calculateMatchingGenotypesCount(variant,
                        alternateBases))
                .sum();

        Long totalGenotypesCount = variants.stream()
                .flatMap(variant -> variant.getCallsList().stream())
                .mapToLong(Variants.Call::getGenotypeLikelihoodCount).sum();

        return (totalGenotypesCount == 0) ? null : ((double) matchingGenotypesCount / totalGenotypesCount);
    }

    private long calculateMatchingGenotypesCount(Variant variant, String alternateBases) {
        int requestedGenotype = variant.getAlternateBasesList().indexOf(alternateBases) + 1;
        return variant.getCallsList()
                .stream()
                .map(Variants.Call::getGenotypeLikelihoodList)
                .flatMap(List::stream)
                .filter(genotype -> genotype.equals(Double.valueOf(requestedGenotype)))
                .count();
    }

    private VariantSet getVariantSetToSearch(String datasetId, String assemblyId) throws BeaconAlleleRequestException {
        // DatasetId = VariantSetId.
        VariantAnnotationSet annotationSet = loadVariantAnnotationSet(datasetId);

        String variantSetId = annotationSet.getVariantSetId();
        VariantSet variantSet = loadVariantSet(variantSetId);

        return isVariantSetMatchesRequested(variantSet, assemblyId) ? variantSet : null;
    }

    private boolean isVariantSetMatchesRequested(VariantSet variantSet, String assemblyId) throws BeaconAlleleRequestException {
        ReferenceSet referenceSet = loadReferenceSet(variantSet.getReferenceSetId());
        return StringUtils.equals(referenceSet.getAssemblyId(), assemblyId);
    }

    private boolean isVariantMatchesRequested(Variant variant, String referenceBases, String alternateBases) {
        return StringUtils.equals(variant.getReferenceBases(), referenceBases) && variant.getAlternateBasesList()
                .contains(alternateBases);
    }

    private List<Variant> loadVariants(String variantSetId, String referenceName, long start) throws BeaconAlleleRequestException {
        try {
            return ga4ghClient.searchVariants(variantSetId, referenceName, start);
        } catch (Ga4ghClientException e) {
            BeaconAlleleRequestException alleleRequestException = new BeaconAlleleRequestException(
                    "Couldn't load reference set with id %s.",
                    Reason.CONN_ERR,
                    null);
            alleleRequestException.initCause(e);
            throw alleleRequestException;
        }
    }

    private VariantSet loadVariantSet(String id) throws BeaconAlleleRequestException {
        try {
            return ga4ghClient.loadVariantSet(id);
        } catch (Ga4ghClientException e) {
            BeaconAlleleRequestException alleleRequestException = new BeaconAlleleRequestException(String.format(
                    "Couldn't load variant set for id %s.",
                    id), Reason.CONN_ERR, null);
            alleleRequestException.initCause(e);
            throw alleleRequestException;
        }
    }

    private VariantAnnotationSet loadVariantAnnotationSet(String id) throws BeaconAlleleRequestException {
        try {
            return ga4ghClient.loadVariantAnnotationSet(id);
        } catch (Ga4ghClientException e) {
            BeaconAlleleRequestException alleleRequestException = new BeaconAlleleRequestException(String.format(
                    "Couldn't load variant annotation set for id %s.",
                    id), Reason.CONN_ERR, null);
            alleleRequestException.initCause(e);
            throw alleleRequestException;
        }
    }

    private ReferenceSet loadReferenceSet(String id) throws BeaconAlleleRequestException {
        try {
            return ga4ghClient.loadReferenceSet(id);
        } catch (Ga4ghClientException e) {
            BeaconAlleleRequestException alleleRequestException = new BeaconAlleleRequestException(
                    "Couldn't load reference set with id %s.",
                    Reason.CONN_ERR,
                    null);
            alleleRequestException.initCause(e);
            throw alleleRequestException;
        }
    }

    private CallSet loadCallSet(String id) throws BeaconAlleleRequestException {
        try {
            return ga4ghClient.loadCallSet(id);
        } catch (Ga4ghClientException e) {
            BeaconAlleleRequestException alleleRequestException = new BeaconAlleleRequestException(
                    "Couldn't load call set with id %s.",
                    Reason.CONN_ERR,
                    null);
            alleleRequestException.initCause(e);
            throw alleleRequestException;
        }
    }

    @Override
    public void initAdapter(AdapterConfig adapterConfig) {
        initGa4ghClient(adapterConfig);
    }

    @Override
    public Beacon getBeacon() throws BeaconException {
        return SAMPLE_BEACON;
    }

    @Override
    public BeaconAlleleResponse getBeaconAlleleResponse(BeaconAlleleRequest request) throws BeaconException {
        try {
            BeaconAlleleResponse response = doGetBeaconAlleleResponse(request.getReferenceName(),
                    request.getStart(),
                    request.getReferenceBases(),
                    request.getAlternateBases(),
                    request.getAssemblyId(),
                    request.getDatasetIds(),
                    request.getIncludeDatasetResponses());
            response.setAlleleRequest(request);
            return response;
        } catch (BeaconAlleleRequestException e) {
            e.setRequest(request);
            throw e;
        }
    }

    @Override
    public BeaconAlleleResponse getBeaconAlleleResponse(String referenceName,
                                                        Long start,
                                                        String referenceBases,
                                                        String alternateBases,
                                                        String assemblyId,
                                                        List<String> datasetIds,
                                                        Boolean includeDatasetResponses) throws BeaconException {
        BeaconAlleleRequest request = createRequest(referenceName,
                start,
                referenceBases,
                alternateBases,
                assemblyId,
                datasetIds,
                includeDatasetResponses);
        return getBeaconAlleleResponse(request);
    }

    /**
     * Works the same way as the Java 8 stream API map method, but can throw {@link BeaconAlleleRequestException}.
     */
    public <T, R> List<R> map(List<T> list, FunctionThrowingAlleleRequestException<? super T, ? extends R> mapper) throws BeaconAlleleRequestException {
        return map(list.stream(), mapper);
    }

    /**
     * Works the same way as the Java 8 stream API map method, but can throw {@link BeaconAlleleRequestException}.
     */
    public <T, R> List<R> map(Stream<T> stream, FunctionThrowingAlleleRequestException<? super T, ? extends R> mapper) throws BeaconAlleleRequestException {
        List<R> result = new ArrayList<>();

        Iterator<T> it = stream.iterator();
        while (it.hasNext()) {
            R mappedItem = mapper.apply(it.next());
            result.add(mappedItem);
        }

        return result;
    }
}