package org.openstack.atlas.api.validation.expectation;

public interface EmptyExpectation {
    OngoingExpectation<FinalizedExpectation> must();
}
