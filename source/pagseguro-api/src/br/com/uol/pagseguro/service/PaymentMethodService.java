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
import java.util.List;

import br.com.uol.pagseguro.domain.Credentials;
import br.com.uol.pagseguro.domain.Error;
import br.com.uol.pagseguro.domain.direct.PaymentMethods;
import br.com.uol.pagseguro.enums.HttpStatus;
import br.com.uol.pagseguro.exception.PagSeguroServiceException;
import br.com.uol.pagseguro.logs.Log;
import br.com.uol.pagseguro.parser.direct.PaymentMethodsParser;
import br.com.uol.pagseguro.utils.HttpConnection;
import br.com.uol.pagseguro.xmlparser.ErrorsParser;

/**
 * 
 * Class Payment Method Service
 */
public class PaymentMethodService {

    /**
     * @var Log
     */
    private static Log log = new Log(PaymentMethodService.class);

    /**
     * 
     * @param ConnectionData
     *            connectionData
     * @param publicKey
     * @return string
     * @throws PagSeguroServiceException
     */
    public static String buildPaymentMethodsUrl(ConnectionData connectionData, String publicKey)
            throws PagSeguroServiceException {
        return connectionData.getPaymentMethodsUrl() + "?publicKey=" + publicKey;
    }

    /**
     * 
     * @param credentials
     * @param publicKey
     * @return PaymentMethods
     * @throws PagSeguroServiceException
     */
    public static PaymentMethods getPaymentMethods(Credentials credentials, String publicKey)
            throws PagSeguroServiceException {

        log.info("PaymentMethodService.getPaymentMethods - begin");

        ConnectionData connectionData = new ConnectionData(credentials);

        String url = buildPaymentMethodsUrl(connectionData, publicKey);

        HttpConnection connection = new HttpConnection();
        HttpStatus httpCodeStatus = null;

        HttpURLConnection response = connection.get(url, connectionData.getServiceTimeout(),
                connectionData.getCharset());

        try {

            httpCodeStatus = HttpStatus.fromCode(response.getResponseCode());
            if (httpCodeStatus == null) {
                throw new PagSeguroServiceException("Connection Timeout");
            } else if (HttpURLConnection.HTTP_OK == httpCodeStatus.getCode().intValue()) {

                PaymentMethods paymentMethods = new PaymentMethods(
                        PaymentMethodsParser.parse(response.getInputStream()));

                log.info("PaymentMethodService.getPaymentMethods() - end");

                return paymentMethods;

            } else if (HttpURLConnection.HTTP_BAD_REQUEST == httpCodeStatus.getCode().intValue()) {

                List<Error> errors = ErrorsParser.readErrosXml(response.getErrorStream());

                PagSeguroServiceException exception = new PagSeguroServiceException(httpCodeStatus, errors);

                log.error(String.format("PaymentMethodService.getPaymentMethods() - error %1s", exception.getMessage()));

                throw exception;

            } else {
                throw new PagSeguroServiceException(httpCodeStatus);
            }
        } catch (PagSeguroServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error(String.format("PaymentMethodService.getPaymentMethods() - error %1s", e.getMessage()));

            throw new PagSeguroServiceException(httpCodeStatus, e);
        } finally {
            response.disconnect();
        }

    }
}