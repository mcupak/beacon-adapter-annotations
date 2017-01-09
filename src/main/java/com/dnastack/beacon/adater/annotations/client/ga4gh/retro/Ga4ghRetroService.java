package com.dnastack.beacon.adater.annotations.client.ga4gh.retro;

import ga4gh.AlleleAnnotations.VariantAnnotationSet;
import ga4gh.References.ReferenceSet;
import ga4gh.VariantServiceOuterClass.SearchVariantsRequest;
import ga4gh.VariantServiceOuterClass.SearchVariantsResponse;
import ga4gh.Variants.CallSet;
import ga4gh.Variants.VariantSet;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * @author Artem (tema.voskoboynick@gmail.com)
 * @author Miro Cupak (mirocupak@gmail.com)
 * @version 1.0
 */
public interface Ga4ghRetroService {

    String VARIANTS_SEARCH_PATH = "variants/search";
    String VARIANT_ANNOTATIONS_SETS_GET_PATH = "variantannotationsets";
    String VARIANT_SETS_GET_PATH = "variantsets";
    String REFERENCE_SETS_GET_PATH = "referencesets";
    String CALL_SETS_GET_PATH = "callsets";

    String VARIANT_ANNOTATION_SETS_ID_PARAM = "id";
    String VARIANT_SETS_ID_PARAM = "id";
    String REFERENCE_SETS_ID_PARAM = "id";
    String CALL_SET_ID_PARAM = "id";

    @POST(VARIANTS_SEARCH_PATH)
    Call<SearchVariantsResponse> searchVariants(@Body SearchVariantsRequest request);

    @GET(REFERENCE_SETS_GET_PATH + "/{id}")
    Call<ReferenceSet> loadReferenceSet(@Path(REFERENCE_SETS_ID_PARAM) String id);

    @GET(VARIANT_ANNOTATIONS_SETS_GET_PATH + "/{id}")
    Call<VariantAnnotationSet> loadVariantAnnotationSet(@Path(VARIANT_ANNOTATION_SETS_ID_PARAM) String id);

    @GET(VARIANT_SETS_GET_PATH + "/{id}")
    Call<VariantSet> loadVariantSet(@Path(VARIANT_SETS_ID_PARAM) String id);

    @GET(CALL_SETS_GET_PATH + "/{id}")
    Call<CallSet> loadCallSet(@Path(CALL_SET_ID_PARAM) String id);
}

