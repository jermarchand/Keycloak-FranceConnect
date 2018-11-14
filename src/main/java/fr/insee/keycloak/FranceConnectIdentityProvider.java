package fr.insee.keycloak;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.HMACProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.services.resources.RealmsResource;

public class FranceConnectIdentityProvider extends OIDCIdentityProvider
    implements SocialIdentityProvider<OIDCIdentityProviderConfig> {



  public FranceConnectIdentityProvider(KeycloakSession session, OIDCIdentityProviderConfig config) {
    super(session, config);

  }


  @Override
  public Response keycloakInitiatedBrowserLogout(KeycloakSession session,
      UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {
    if (getConfig().getLogoutUrl() == null || getConfig().getLogoutUrl().trim().equals("")) {
      return null;
    }
    String idToken = userSession.getNote(FEDERATED_ID_TOKEN);
    if (idToken != null && getConfig().isBackchannelSupported()) {
      backchannelLogout(userSession, idToken);
      return null;
    } else {
      String sessionId = userSession.getId();
      UriBuilder logoutUri =
          UriBuilder.fromUri(getConfig().getLogoutUrl()).queryParam("state", sessionId);
      if (idToken != null) {
        logoutUri.queryParam("id_token_hint", idToken);
      }
      String redirect =
          RealmsResource.brokerUrl(uriInfo).path(IdentityBrokerService.class, "getEndpoint")
              .path(OIDCEndpoint.class, "logoutResponse")
              .build(realm.getName(), getConfig().getAlias()).toString();
      logoutUri.queryParam("post_logout_redirect_uri", redirect);
      Response response = Response.status(302).location(logoutUri.build()).build();
      return response;
    }
  }


  @Override
  protected boolean verify(JWSInput jws) {
    if (!getConfig().isValidateSignature()) {
      return true;
    }
    return HMACProvider.verify(jws, getConfig().getClientSecret().getBytes());
  }



}