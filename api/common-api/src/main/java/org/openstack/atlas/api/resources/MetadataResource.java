package org.openstack.atlas.api.resources;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openstack.atlas.api.helpers.ResponseFactory;
import org.openstack.atlas.api.repository.ValidatorRepository;
import org.openstack.atlas.api.resources.providers.CommonDependencyProvider;
import org.openstack.atlas.api.validation.context.HttpRequestType;
import org.openstack.atlas.api.validation.results.ValidatorResult;
import org.openstack.atlas.docs.loadbalancers.api.v1.LoadbalancerMetadata;
import org.openstack.atlas.service.domain.entities.LoadBalancer;
import org.openstack.atlas.service.domain.entities.LoadbalancerMeta;
import org.openstack.atlas.service.domain.exceptions.BadRequestException;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static javax.ws.rs.core.MediaType.*;

public class MetadataResource extends CommonDependencyProvider {
    private final Log LOG = LogFactory.getLog(MetadataResource.class);
    private MetaResource metaResource;
    private HttpHeaders requestHeaders;
    private Integer accountId;
    private Integer loadBalancerId;

    @POST
    @Consumes({APPLICATION_XML, APPLICATION_JSON})
    public Response createMetadata(LoadbalancerMetadata metadata) {
        ValidatorResult result = ValidatorRepository.getValidatorFor(LoadbalancerMetadata.class).validate(metadata, HttpRequestType.POST);

        if (!result.passedValidation()) {
            return getValidationFaultResponse(result);
        }

        try {
            loadBalancerService.get(loadBalancerId, accountId);

            List<LoadbalancerMeta> domainLoadbalancerMetaList = new ArrayList<LoadbalancerMeta>();
            for (org.openstack.atlas.docs.loadbalancers.api.v1.LoadbalancerMeta meta : metadata.getLoadbalancerMetas()) {
                domainLoadbalancerMetaList.add(dozerMapper.map(meta, LoadbalancerMeta.class));
            }

            Set<LoadbalancerMeta> dbMetadata = loadbalancerMetadataService.createLoadbalancerMetadata(accountId, loadBalancerId, domainLoadbalancerMetaList);

            org.openstack.atlas.docs.loadbalancers.api.v1.LoadbalancerMetadata returnMetadata = new org.openstack.atlas.docs.loadbalancers.api.v1.LoadbalancerMetadata();
            for (LoadbalancerMeta loadbalancerMeta : dbMetadata) {
                returnMetadata.getLoadbalancerMetas().add(dozerMapper.map(loadbalancerMeta, org.openstack.atlas.docs.loadbalancers.api.v1.LoadbalancerMeta.class));
            }

            return Response.status(Response.Status.OK).entity(returnMetadata).build();
        } catch (Exception e) {
            return ResponseFactory.getErrorResponse(e, null, null);
        }
    }

    @GET
    @Produces({APPLICATION_XML, APPLICATION_JSON, APPLICATION_ATOM_XML})
    public Response retrieveMetadata(){
        Set<LoadbalancerMeta> domainLoadbalancerMetaSet;
        org.openstack.atlas.docs.loadbalancers.api.v1.LoadbalancerMetadata returnMetadata = new org.openstack.atlas.docs.loadbalancers.api.v1.LoadbalancerMetadata();
        try {
            domainLoadbalancerMetaSet = loadbalancerMetadataService.getLoadbalancerMetadataByAccountIdLoadBalancerId(accountId, loadBalancerId);
            for (LoadbalancerMeta domainLoadbalancerMeta : domainLoadbalancerMetaSet) {
                returnMetadata.getLoadbalancerMetas().add(dozerMapper.map(domainLoadbalancerMeta, org.openstack.atlas.docs.loadbalancers.api.v1.LoadbalancerMeta.class));
            }
            return Response.status(Response.Status.OK).entity(returnMetadata).build();
        } catch (Exception e) {
            return ResponseFactory.getErrorResponse(e, null, null);
        }
    }

    @DELETE
    @Produces({APPLICATION_XML, APPLICATION_JSON})
    public Response deleteMetadata(@QueryParam("id") List<Integer> metaIds) {
        List<String> validationErrors;
        Collections.sort(metaIds);
        LoadBalancer dlb = new LoadBalancer();
        dlb.setId(loadBalancerId);
        dlb.setAccountId(accountId);

        try {
            if (metaIds.isEmpty()) {
                BadRequestException badRequestException = new BadRequestException("Must supply one or more id's to process this request.");
                return ResponseFactory.getErrorResponse(badRequestException, null, null);
            }

            validationErrors = loadbalancerMetadataService.prepareForLoadbalancerMetadataDeletion(accountId, loadBalancerId, metaIds);
            if (!validationErrors.isEmpty()) {
                return getValidationFaultResponse(validationErrors);
            }
            
            loadbalancerMetadataService.deleteMetadata(dlb, metaIds);
        } catch (Exception ex) {
            return ResponseFactory.getErrorResponse(ex, null, null);
        }
        return Response.status(200).build();
    }

    @Path("{id: [-+]?[1-9][0-9]*}")
    public MetaResource retrieveNodeResource(@PathParam("id") int id) {
        metaResource.setRequestHeaders(requestHeaders);
        metaResource.setId(id);
        metaResource.setAccountId(accountId);
        metaResource.setLoadBalancerId(loadBalancerId);
        return metaResource;
    }

    public void setMetaResource(MetaResource metaResource) {
        this.metaResource = metaResource;
    }

    public void setRequestHeaders(HttpHeaders requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public void setLoadBalancerId(Integer loadBalancerId) {
        this.loadBalancerId = loadBalancerId;
    }
}
