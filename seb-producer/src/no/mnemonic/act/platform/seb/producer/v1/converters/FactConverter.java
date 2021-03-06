package no.mnemonic.act.platform.seb.producer.v1.converters;

import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.seb.model.v1.FactSEB;
import no.mnemonic.act.platform.seb.producer.v1.resolvers.*;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.inject.Inject;
import java.util.function.Function;

public class FactConverter implements Function<FactRecord, FactSEB> {

  private final FactTypeInfoResolver typeResolver;
  private final FactInfoResolver inReferenceToResolver;
  private final OrganizationInfoResolver organizationResolver;
  private final OriginInfoResolver originResolver;
  private final SubjectInfoResolver addedByResolver;
  private final ObjectInfoConverter objectConverter;
  private final AclEntryConverter aclEntryConverter;

  @Inject
  public FactConverter(
          FactTypeInfoResolver typeResolver,
          FactInfoResolver inReferenceToResolver,
          OrganizationInfoResolver organizationResolver,
          OriginInfoResolver originResolver,
          SubjectInfoResolver addedByResolver,
          ObjectInfoConverter objectConverter,
          AclEntryConverter aclEntryConverter
  ) {
    this.typeResolver = typeResolver;
    this.inReferenceToResolver = inReferenceToResolver;
    this.organizationResolver = organizationResolver;
    this.originResolver = originResolver;
    this.addedByResolver = addedByResolver;
    this.objectConverter = objectConverter;
    this.aclEntryConverter = aclEntryConverter;
  }

  @Override
  public FactSEB apply(FactRecord record) {
    if (record == null) return null;

    return FactSEB.builder()
            .setId(record.getId())
            .setType(typeResolver.apply(record.getTypeID()))
            .setValue(record.getValue())
            .setInReferenceTo(inReferenceToResolver.apply(record.getInReferenceToID()))
            .setOrganization(organizationResolver.apply(record.getOrganizationID()))
            .setOrigin(originResolver.apply(record.getOriginID()))
            .setAddedBy(addedByResolver.apply(record.getAddedByID()))
            .setAccessMode(ObjectUtils.ifNotNull(record.getAccessMode(), mode -> FactSEB.AccessMode.valueOf(mode.name())))
            .setTrust(record.getTrust())
            .setConfidence(record.getConfidence())
            .setTimestamp(record.getTimestamp())
            .setLastSeenTimestamp(record.getLastSeenTimestamp())
            .setSourceObject(objectConverter.apply(record.getSourceObject()))
            .setDestinationObject(objectConverter.apply(record.getDestinationObject()))
            .setBidirectionalBinding(record.isBidirectionalBinding())
            .setFlags(SetUtils.set(record.getFlags(), flag -> FactSEB.Flag.valueOf(flag.name())))
            .setAcl(SetUtils.set(record.getAcl(), aclEntryConverter))
            .build();
  }
}
