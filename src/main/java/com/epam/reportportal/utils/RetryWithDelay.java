/*
 * Copyright 2017 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/client
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.utils;

import io.reactivex.functions.Predicate;

import static java.lang.Thread.sleep;

/**
 * Retry with Delay and attempts limits
 *
 * @author Andrei Varabyeu
 */
public class RetryWithDelay implements Predicate<Throwable> {

    private final Predicate<? super Throwable> predicate;
    private final long maxRetries;
    private final long retryDelayMillis;
    private int retryCount;

    public RetryWithDelay(Predicate<? super Throwable> predicate, final long maxRetries, final long retryDelayMillis) {
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
        this.retryCount = 0;
        this.predicate = predicate;
    }

    @Override
    public boolean test(final Throwable throwable) throws Exception {
        try {
            //check whether we should retry this exception
            if (!predicate.test(throwable)) {
                return false;
            }
        } catch (Exception e) {
            //pass the error if smth goes wrong
            return false;
        }

        if (++retryCount < maxRetries) {
            sleep(retryDelayMillis);
            return true;
        }

        // Max retries hit. Just pass the error along.
        return false;
    }
}
