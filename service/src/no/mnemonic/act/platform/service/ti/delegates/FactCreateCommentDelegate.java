package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.FactComment;
import no.mnemonic.act.platform.api.request.v1.CreateFactCommentRequest;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactCommentRecord;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.response.FactCommentResponseConverter;
import no.mnemonic.act.platform.service.ti.resolvers.request.FactRequestResolver;
import no.mnemonic.commons.utilities.collections.ListUtils;

import javax.inject.Inject;
import java.util.Objects;
import java.util.UUID;

public class FactCreateCommentDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final ObjectFactDao objectFactDao;
  private final FactRequestResolver factRequestResolver;
  private final FactCommentResponseConverter factCommentResponseConverter;

  @Inject
  public FactCreateCommentDelegate(TiSecurityContext securityContext,
                                   ObjectFactDao objectFactDao,
                                   FactRequestResolver factRequestResolver,
                                   FactCommentResponseConverter factCommentResponseConverter) {
    this.securityContext = securityContext;
    this.objectFactDao = objectFactDao;
    this.factRequestResolver = factRequestResolver;
    this.factCommentResponseConverter = factCommentResponseConverter;
  }

  public FactComment handle(CreateFactCommentRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Fetch Fact and verify that it exists.
    FactRecord fact = factRequestResolver.resolveFact(request.getFact());
    // Verify that user is allowed to access the Fact.
    securityContext.checkReadPermission(fact);
    // Verify that user is allowed to comment on the Fact.
    securityContext.checkPermission(TiFunctionConstants.addThreatIntelFactComment, fact.getOrganizationID());
    // Verify that the 'replyTo' comment exists.
    verifyReplyToCommentExists(fact, request);
    // Save comment and return it to the user.
    return factCommentResponseConverter.apply(saveComment(fact, request));
  }

  private void verifyReplyToCommentExists(FactRecord fact, CreateFactCommentRequest request) throws InvalidArgumentException {
    if (request.getReplyTo() == null) return;

    boolean exists = ListUtils.list(fact.getComments())
            .stream()
            .anyMatch(comment -> Objects.equals(comment.getId(), request.getReplyTo()));

    if (!exists) {
      throw new InvalidArgumentException()
              .addValidationError("Comment does not exist.", "comment.no.exists", "replyTo", request.getReplyTo().toString());
    }
  }

  private FactCommentRecord saveComment(FactRecord fact, CreateFactCommentRequest request) {
    FactCommentRecord comment = new FactCommentRecord()
            .setId(UUID.randomUUID())
            .setReplyToID(request.getReplyTo())
            .setOriginID(securityContext.getCurrentUserID())
            .setComment(request.getComment())
            .setTimestamp(System.currentTimeMillis());

    return objectFactDao.storeFactComment(fact, comment);
  }
}
