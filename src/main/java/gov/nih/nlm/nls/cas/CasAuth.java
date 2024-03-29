package gov.nih.nlm.nls.cas;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

/**
 * An example Java client to authenticate against CAS using REST services.
 * Please ensure you have followed the necessary setup found on the <a
 * href="http://www.ja-sig.org/wiki/display/CASUM/RESTful+API">JASIG wiki</a>.
 *
 * <pre>
 * ticket = getTicket(casserverurl, apikey, serviceurl);
 * String String document = getProtectedDocument(serviceurl, ticket);
 * </pre>
 *
 * @author <a href="mailto:jieryn@gmail.com">jesse lauren farinacci</a>
 * @since 3.4.2
 * updated to use HTTP component library (http://hc.apache.org/) by Willie Rogers.
 */
public final class CasAuth
{
  private static final Logger LOG = Logger.getLogger(CasAuth.class.getName());

  private CasAuth()
  {
    // static-only access
  }


  /**
   * Obtain a  Single-Use Proxy Ticket from Central Authentication Server (CAS).
   * @param serverurl authentication server
   * @param tgtserverurl ticket granting ticket server
   * @param apikey UTS profile API key
   * @param serviceurl url of service with protected resources
   * @return authentication ticket for service.
   */
  public static String getTicket(final String serverurl,
				 final String tgtserverurl,
				 final String apikey,
				 final String serviceurl)
  {
    notNull(serverurl, "server must not be null");
    notNull(tgtserverurl, "server must not be null");
    notNull(apikey, "api key must not be null");
    notNull(serviceurl, "service must not be null");

    return getServiceTicket(serverurl,
			    getTicketGrantingTicket(tgtserverurl, apikey),
			    serviceurl);
  }

  /**
   * Obtain a Single-Use Proxy Ticket (also known as service ticket).
   * Request for a Service Ticket:
   * <pre>
   *     POST /cas/v1/tickets/{TGT id} HTTP/1.0
   * </pre>
   *     data:
   * <pre>
   *     service={form encoded parameter for the service url}
   * </pre>     
   *    Sucessful Response:
   * <pre>
   *     200 OK
   *     
   *     ST-1-FFDFHDSJKHSDFJKSDHFJKRUEYREWUIFSD2132
   * </pre>
   *
   * @param serverurl authentication server
   * @param ticketGrantingTicket a Proxy Granting Ticket.
   * @param serviceurl url of service with protected resources
   * @return authentication ticket for service.
   *
   */
  private static String getServiceTicket(final String serverurl,
					 final String ticketGrantingTicket,
					 final String serviceurl)
  {
    if (ticketGrantingTicket == null)
      return null;

    RequestConfig requestConfig = RequestConfig.custom()
	.setCookieSpec(CookieSpecs.STANDARD)
	.build();

    final CloseableHttpClient client = HttpClientBuilder.create()
	.setDefaultRequestConfig(requestConfig)
	.build();

    List<NameValuePair> formparams = new ArrayList<NameValuePair>();
    formparams.add(new BasicNameValuePair("service", serviceurl));
    try {
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
      final HttpPost post = new HttpPost(serverurl + "/" + ticketGrantingTicket);
      post.setEntity(entity);
      post.setHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 Firefox/26.0");
      // Create a response handler
      ResponseHandler<String> responseHandler = new BasicResponseHandler();
      String responseBody = client.execute(post, responseHandler);
      // System.out.println("response: " + responseBody);
      return responseBody;
    } catch (final   java.io.UnsupportedEncodingException  e) {
      LOG.warning(e.getMessage());
    } catch (final IOException e) {
      LOG.warning(e.getMessage());
    } finally {
      // When HttpClient instance is no longer needed,
      // shut down the connection manager to ensure
      // immediate deallocation of all system resources
      try {
	client.close();
      } catch (final IOException e) {
	LOG.warning(e.getMessage());
      }
    }
    return null;
  }

  /**
   * Obtain a Proxy Granting Ticket.
   * Response for a Ticket Granting Ticket Resource  
   * <pre>
   *   POST /cas/v1/api-key HTTP/1.0
   * </pre>
   *   data:
   * <pre>
   *   apikey
   * </pre>
   *  Successful Response:
   * <pre>
   *   201 Created
   *   Location: http://serviceurl/cas/v1/tickets/{TGT id}
   * </pre>
   * @param serverurl authentication server
   * @param apikey UTS profile API key
   * @return a Proxy Granting Ticket.
   */
  private static String getTicketGrantingTicket(final String serverurl,
						final String apikey)
  {
    RequestConfig requestConfig = RequestConfig.custom()
	.setCookieSpec(CookieSpecs.STANDARD)
	.build();

    final CloseableHttpClient client = HttpClientBuilder.create()
	.setDefaultRequestConfig(requestConfig)
	.build();

    List<NameValuePair> formparams = new ArrayList<NameValuePair>();
    formparams.add(new BasicNameValuePair("apikey", apikey));
    try {
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
      final HttpPost post = new HttpPost(serverurl);
      post.setEntity(entity);
      post.setHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 Firefox/26.0");
      ResponseHandler<String> responseHandler = new BasicResponseHandler();
      String response0 = client.execute(post, responseHandler);
      String response = response0.replace("\n", "");
      // System.out.println("response: " + response);
      final Matcher matcher = Pattern.compile(".*action=\".*/(.*?)\".*")
	.matcher(response);
      if (matcher.find()) {
	// System.out.println("ticket: " +  matcher.group(1));
	return matcher.group(1);
      } else {
	throw new RuntimeException("error extracting ticket granting ticket.");
      }
    } catch (final java.io.UnsupportedEncodingException  e) {
      LOG.warning(e.getMessage());
    } catch (final IOException e) {
      LOG.warning(e.getMessage());
    } finally {
      try {
	client.close();
      } catch (final IOException e) {
	LOG.warning(e.getMessage());
      }
    }
    return null;
  }

  /**
   * Request for protected document using service ticket.
   * <pre>
   *    GET {resource url}?ticket={service ticket} HTTP/1.0
   * </pre>
   * example:
   * <pre>
   *   http://wsd.nlm.nih.gov/Restricted/Non-Reviewed_Results/index.shtml?ticket=ST-1-FFDFHDSJKHSDFJKSDHFJKRUEYREWUIFSD2132
   * </pre>
   *   Successful Response:
   * <pre>
   *   200
   * </pre>
   *    document is returned in body of response.
   * <p>
   * @param service url of service with protected resources
   * @param ticket authentication ticket for service.
   * @return document or null if authentication invalid.
   */
  static String getProtectedDocument(String service, String ticket)
  {
    try {
      final CloseableHttpClient client = HttpClients.createDefault();
      final HttpGet getReq = new HttpGet(service + "?ticket=" + ticket);
      getReq.setHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 Firefox/26.0");
      ResponseHandler<String> responseHandler = new BasicResponseHandler();
      String responseBody = client.execute(getReq, responseHandler);
      client.close();
      return responseBody;
    } catch (final IOException e) {
      LOG.warning(e.getMessage());
    }
    return null;
  }

  private static void notNull(final Object object, final String message)
  {
    if (object == null)
      throw new IllegalArgumentException(message);
  }

  public static void main(final String[] args)
  {
    // final String serverurl = "http://localhost:8080/cas/v1/tickets";
    // final String tgtserverurl = "http://localhost:8080/cas/v1/api-key";
    // final String apikey = "some hex number"
    // final String serviceurl = "http://localhost:8080/service";

    if (args.length > 0) {
      final String serverurl = "https://utslogin.nlm.nih.gov/cas/v1/tickets";
      final String tgtserverurl = "https://utslogin.nlm.nih.gov/cas/v1/api-key";
      final String apikey = args[0];
      String serviceurl;
      if (args.length > 1) {
	serviceurl = args[1];
      } else {
	serviceurl = "https://wsd.nlm.nih.gov/Restricted/Non-Reviewed_Results/index.shtml";
      }
      String ticket = getTicket(serverurl, tgtserverurl, apikey, serviceurl);
      LOG.info("Service Ticket: " + ticket);
      String document = getProtectedDocument(serviceurl, ticket);
      LOG.info(document);
    } else {
      System.out.println("usage: cas.Client apikey");
    }
  }
}
