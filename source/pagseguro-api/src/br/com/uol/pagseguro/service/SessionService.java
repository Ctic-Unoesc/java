/*
 ************************************************************************
 Copyright [2011] [PagSeguro Internet Ltda.]

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 ************************************************************************
 */

package br.com.uol.pagseguro.service;

import java.net.HttpURLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import br.com.uol.pagseguro.domain.Credentials;
import br.com.uol.pagseguro.enums.HttpStatus;
import br.com.uol.pagseguro.exception.PagSeguroServiceException;
import br.com.uol.pagseguro.logs.Log;
import br.com.uol.pagseguro.utils.HttpConnection;
import br.com.uol.pagseguro.xmlparser.XMLParserUtils;

/**
 * 
 * Class Session Service
 */
public class SessionService {

    private SessionService() {
    }

    /**
     * @var Log
     */
    private static Log log = new Log(SessionService.class);

    /**
     * Build a Direct Payment Session Request Url
     * 
     * @param ConnectionData
     *            connectionData
     * @return string
     * @throws PagSeguroServiceException
     */
    public static String buildPaymentSessionRequestUrl(ConnectionData connectionData) throws PagSeguroServiceException {
        return connectionData.getPaymentSessionUrl() + "?" + connectionData.getCredentialsUrlQuery();
    }

    public static String createPaymentSession(Credentials credentials) throws PagSeguroServiceException {
        log.info(String.format("SessionService.createPaymentSession( %s ) - begin", credentials.toString()));

        ConnectionData connectionData = new ConnectionData(credentials);

        String url = SessionService.buildPaymentSessionRequestUrl(connectionData);

        HttpConnection connection = new HttpConnection();
        HttpStatus httpCodeStatus = null;

        HttpURLConnection response = connection.get(url, connectionData.getServiceTimeout(),
                connectionData.getCharset());

        try {

            httpCodeStatus = HttpStatus.fromCode(response.getResponseCode());
            if (httpCodeStatus == null) {
                throw new PagSeguroServiceException("Connection Timeout");
            } else if (HttpURLConnection.HTTP_OK == httpCodeStatus.getCode().intValue()) {

                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

                InputSource is = new InputSource(response.getInputStream());
                Document doc = dBuilder.parse(is);

                String tagValue = null;

                Element paymentSessionElement = doc.getDocumentElement();

                // parsing <startPaymentSessionResult><sessionId>
                tagValue = XMLParserUtils.getTagValue("sessionId", paymentSessionElement);

                log.info(String.format("SessionService.createPaymentSession( %1s ) - end  %2s )",
                        credentials.toString(), tagValue));

                return tagValue;

            } else if (HttpURLConnection.HTTP_UNAUTHORIZED == httpCodeStatus.getCode().intValue()) {

                PagSeguroServiceException exception = new PagSeguroServiceException(httpCodeStatus);

                log.error(String.format("SessionService.createPaymentSession( %1s ) - error %2s",
                        credentials.toString(), exception.getMessage()));

                throw exception;

            } else {

                throw new PagSeguroServiceException(httpCodeStatus);
            }

        } catch (PagSeguroServiceException e) {
            throw e;
        } catch (Exception e) {

            log.error(String.format("SessionService.createPaymentSession( %1s ) - error %2s", credentials.toString(),
                    e.getMessage()));

            throw new PagSeguroServiceException(httpCodeStatus, e);

        } finally {
            response.disconnect();
        }

    }

}