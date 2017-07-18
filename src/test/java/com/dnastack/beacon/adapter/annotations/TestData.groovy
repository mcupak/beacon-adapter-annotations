package com.dnastack.beacon.adapter.annotations

import com.google.protobuf.ListValue
import com.google.protobuf.Value

import static ga4gh.AlleleAnnotations.VariantAnnotationSet
import static ga4gh.References.ReferenceSet
import static ga4gh.VariantServiceOuterClass.SearchVariantsRequest
import static ga4gh.VariantServiceOuterClass.SearchVariantsResponse
import static ga4gh.Variants.*

/**
 * @author Artem (tema.voskoboynick@gmail.com)
 * @version 1.0
 */
public class TestData {

    def public static final TEST_CALL_SET_1 = CallSet.newBuilder()
            .setId("test-callset-1")
            .setBiosampleId("test-bio-sample-1")
            .build()

    def public static final TEST_CALL_SET_2 = CallSet.newBuilder()
            .setId("test-callset-2")
            .setBiosampleId("test-bio-sample-2")
            .build()

    def public static final TEST_CALL_1 = Call.newBuilder()
            .setCallSetId(TEST_CALL_SET_1.id)
            .setGenotype(ListValue.newBuilder()
            .addValues(Value.newBuilder().setNumberValue(1.0))
            .addValues(Value.newBuilder().setNumberValue(2.0)))
            .build()

    def public static final TEST_CALL_2 = Call.newBuilder()
            .setCallSetId(TEST_CALL_SET_2.id)
            .setGenotype(ListValue.newBuilder()
            .addValues(Value.newBuilder().setNumberValue(3.0))
            .addValues(Value.newBuilder().setNumberValue(4.0)))
            .build()

    def public static final TEST_VARIANT = Variant.newBuilder()
            .setId("test-variant")
            .setReferenceBases("test-reference-bases")
            .addAllAlternateBases(["test-alternate-base-1", "test-alternate-base-2", "test-alternate-base-3"])
            .addAllCalls([TEST_CALL_1, TEST_CALL_2])
            .build()

    def public static final TEST_REFERENCE_SET = ReferenceSet.newBuilder()
            .setId("test-reference-set")
            .setAssemblyId("test-assembly")
            .build()

    def public static final TEST_VARIANT_SET = VariantSet.newBuilder()
            .setId("test-variant-set")
            .setReferenceSetId(TEST_REFERENCE_SET.id)
            .build()

    def public static final TEST_VARIANT_ANNOTATION_SET = VariantAnnotationSet.newBuilder()
            .setId("test-variant-annotation-set")
            .setVariantSetId(TEST_VARIANT_SET.id)
            .build()

    def public static final SEARCH_VARIANTS_TEST_RESPONSE = SearchVariantsResponse.newBuilder()
            .addVariants(TEST_VARIANT)
            .build()

    def public static final SEARCH_VARIANTS_TEST_REQUEST = SearchVariantsRequest.newBuilder()
            .setVariantSetId(TEST_VARIANT_SET.id)
            .setReferenceName("test-reference-name")
            .setStart(100)
            .setEnd(101)
            .build()
}