package br.com.uol.pagseguro.service;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import br.com.uol.pagseguro.domain.Credentials;
import br.com.uol.pagseguro.domain.Error;
import br.com.uol.pagseguro.domain.Transaction;
import br.com.uol.pagseguro.domain.direct.PaymentRequest;
import br.com.uol.pagseguro.enums.HttpStatus;
import br.com.uol.pagseguro.exception.PagSeguroServiceException;
import br.com.uol.pagseguro.logs.Log;
import br.com.uol.pagseguro.parser.TransactionParser;
import br.com.uol.pagseguro.utils.HttpConnection;
import br.com.uol.pagseguro.xmlparser.ErrorsParser;

/**
 * 
 * Class Direct Payment Service
 */
public class DirectPaymentService {

    /**
     * @var Log
     */
    private static Log log = new Log(DirectPaymentService.class);

    /**
     * Build a direct payment url
     * 
     * @param ConnectionData
     *            connectionData
     * @return string
     * @throws PagSeguroServiceException
     */
    private static String buildDirectPaymentRequestUrl(ConnectionData connectionData) throws PagSeguroServiceException {
        return connectionData.getDirectPaymentUrl() + "?" + connectionData.getCredentialsUrlQuery();
    }

    /**
     * Create a DirectTransaction
     * 
     * @param credentials
     * @param payment
     * @return string
     * @throws PagSeguroServiceException
     */
    public static Transaction createDirectTransaction(Credentials credentials, PaymentRequest payment)
            throws PagSeguroServiceException {
        log.info(String.format("DirectPaymentService.createDirectTransaction( %s ) - begin", payment.toString()));

        ConnectionData connectionData = new ConnectionData(credentials);

        Map<Object, Object> data = payment.getMap();

        String url = buildDirectPaymentRequestUrl(connectionData);

        HttpConnection connection = new HttpConnection();
        HttpStatus httpCodeStatus = null;

        HttpURLConnection response = connection.post(url, data, connectionData.getServiceTimeout(),
                connectionData.getCharset());

        try {

            httpCodeStatus = HttpStatus.fromCode(response.getResponseCode());
            if (httpCodeStatus == null) {
                throw new PagSeguroServiceException("Connection Timeout");
            } else if (HttpURLConnection.HTTP_OK == httpCodeStatus.getCode().intValue()) {

                Transaction transaction = TransactionParser.readTransaction(response.getInputStream());

                log.info(String.format("DirectPaymentService.createDirectTransaction( %1s ) - end  %2s )",
                        payment.toString(), transaction.getCode()));

                return transaction;

            } else if (HttpURLConnection.HTTP_BAD_REQUEST == httpCodeStatus.getCode().intValue()) {

                List<Error> errors = ErrorsParser.readErrosXml(response.getErrorStream());

                PagSeguroServiceException exception = new PagSeguroServiceException(httpCodeStatus, errors);

                log.error(String.format("DirectPaymentService.createDirectTransaction( %1s ) - error %2s",
                        payment.toString(), exception.getMessage()));

                throw exception;

            } else {

                throw new PagSeguroServiceException(httpCodeStatus);
            }

        } catch (PagSeguroServiceException e) {
            throw e;
        } catch (Exception e) {

            log.error(String.format("DirectPaymentService.createDirectTransaction( %1s ) - error %2s",
                    payment.toString(), e.getMessage()));

            throw new PagSeguroServiceException(httpCodeStatus, e);

        } finally {
            response.disconnect();
        }

    }

}
