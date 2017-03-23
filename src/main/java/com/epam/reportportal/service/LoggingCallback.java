package com.epam.reportportal.service;

import com.epam.ta.reportportal.ws.model.EntryCreatedRS;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import io.reactivex.functions.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Set of logging callback for ReportPortal client
 *
 * @author Andrei Varabyeu
 */
final class LoggingCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportPortal.class);

    private LoggingCallback() {
        //statics only
    }

    /**
     * Logs success
     */
    static final Consumer<OperationCompletionRS> LOG_SUCCESS = new Consumer<OperationCompletionRS>() {
        @Override
        public void accept(OperationCompletionRS rs) throws Exception {
            LOGGER.debug(rs.getResultMessage());
        }
    };

    /**
     * Logs an error
     */
    static final Consumer<Throwable> LOG_ERROR = new Consumer<Throwable>() {
        @Override
        public void accept(Throwable rs) throws Exception {
            LOGGER.error("ReportPortal execution error", rs);
        }
    };

    /**
     * Logs message once some entity creation
     *
     * @param entry Type of entity
     * @return Consumer/Callback
     */
    static Consumer<EntryCreatedRS> logCreated(final String entry) {
        return new Consumer<EntryCreatedRS>() {
            @Override
            public void accept(EntryCreatedRS rs) throws Exception {
                LOGGER.debug("ReportPortal {} with ID '{}' has been created", entry, rs.getId());
            }
        };
    }
}
