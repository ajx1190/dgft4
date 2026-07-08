package com.htc.dgft.scheduler;

import com.htc.dgft.entity.DgftOrmMaster;
import com.htc.dgft.entity.DgftOrmMessageMaster;
import com.htc.dgft.repository.DgftOrmMasterRepository;
import com.htc.dgft.repository.DgftOrmMessageMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * DGFT IRM Enquiry Service Scheduler (Instruction 4).
 *
 * Once the API push scheduler (instruction 3) has pushed batches to the
 * (mock) DGFT end, this scheduler reviews the progress of those IRM details
 * and propagates the staging status back to the main ORM master table:
 *
 *   dgft_orm_master :
 *     FLAG                = "P"
 *     STATUS              = "ACTIVE"
 *     DGFT_FLAG           = "F"
 *     DGFT_STATUS         = "VALIDATED"
 *     MASTER_DETAIL_STATUS = "MSG_PUSH_SUCCESS"
 *
 * It selects message masters that have already been pushed (STATUS = "MSG_PUSH_SUCCESS")
 * and, for every ORM record linked by BANK_UNIQUE_TRANSACTION_ID, applies the
 * status updates above.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DgftEnquiryJob implements Job {

    private final DgftOrmMessageMasterRepository messageMasterRepo;
    private final DgftOrmMasterRepository ormMasterRepo;

    @Override
    @Transactional
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            log.info("Starting DGFT IRM Enquiry Job execution");
            int updated = processEnquiry();
            log.info("DGFT IRM Enquiry Job completed. Updated {} ORM master records", updated);
        } catch (Exception e) {
            log.error("Error executing DGFT IRM Enquiry Job", e);
            throw new JobExecutionException("Enquiry batch failed", e, false);
        }
    }

    /**
     * Reviews pushed message masters and syncs their status back to the ORM master table.
     *
     * @return number of ORM master records updated
     */
    @Transactional
    public int processEnquiry() {
        // Step 1: Find message masters that have been successfully pushed to DGFT
        List<DgftOrmMessageMaster> pushedMasters =
                messageMasterRepo.findByStatus("MSG_PUSH_SUCCESS");

        if (pushedMasters.isEmpty()) {
            log.info("No MSG_PUSH_SUCCESS message masters found to enquire");
            return 0;
        }

        int totalUpdated = 0;

        // Step 2: For each pushed batch, update the linked ORM master records
        for (DgftOrmMessageMaster master : pushedMasters) {
            List<DgftOrmMaster> ormRecords =
                    ormMasterRepo.findByBankUniqueTransactionId(master.getUniqueTxId());

            for (DgftOrmMaster orm : ormRecords) {
                orm.setFlag("P");
                orm.setStatus("ACTIVE");
                orm.setDgftFlag("F");
                orm.setDgftStatus("VALIDATED");
                orm.setMasterDetailStatus("MSG_PUSH_SUCCESS");
            }

            ormMasterRepo.saveAll(ormRecords);
            totalUpdated += ormRecords.size();
        }

        return totalUpdated;
    }
}