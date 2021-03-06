/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.pinsetter.tasks;

import org.candlepin.model.JobCurator;
import org.candlepin.util.Util;

import com.google.inject.Inject;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * JobCleaner removes finished jobs older than yesterday, and failed
 * jobs from 4 days ago.
 */
public class JobCleaner extends KingpinJob {

    private static Logger log = LoggerFactory.getLogger(JobCleaner.class);

    private final int MAX_JOB_AGE_IN_DAYS = 4;

    private JobCurator jobCurator;
    public static final String DEFAULT_SCHEDULE = "0 0 12 * * ?";

    @Inject
    public JobCleaner(JobCurator curator) {
        this.jobCurator = curator;
    }

    @Override
    public void toExecute(JobExecutionContext arg0) throws JobExecutionException {
        // TODO: Configure deadline date to something else..
        // CAUTION: jobCurator uses setDate on the delete query,
        // so all time info is stripped off
        Date deadLineDt = Util.yesterday();
        int oldCompletedJobs = this.jobCurator.cleanUpOldCompletedJobs(deadLineDt);

        Date failedJobDeadLineDt = Util.addDaysToDt(-1 * MAX_JOB_AGE_IN_DAYS);
        int asOf4DaysAgo = this.jobCurator.cleanupAllOldJobs(failedJobDeadLineDt);

        log.debug("Cleaned up {} completed jobs and {} jobs older than {} days old.",
            oldCompletedJobs, asOf4DaysAgo, MAX_JOB_AGE_IN_DAYS);
    }

}
