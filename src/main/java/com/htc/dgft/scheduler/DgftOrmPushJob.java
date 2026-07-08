package com.htc.dgft.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.htc.dgft.entity.DgftOrmMaster;
import com.htc.dgft.entity.DgftOrmMessageDetail;
import com.htc.dgft.entity.DgftOrmMessageMaster;
import com.htc.dgft.repository.DgftOrmMasterRepository;
import com.htc.dgft.repository.DgftOrmMessageDetailRepository;
import com.htc.dgft.repository.DgftOrmMessageMasterRepository;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DgftOrmPushJob implements Job {

    private final DgftOrmMasterRepository masterRepository;
    private final DgftOrmMessageMasterRepository messageMasterRepository;
    private final DgftOrmMessageDetailRepository messageDetailRepository;

    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void execute(JobExecutionContext context) throws JobExecutionException {

        List<DgftOrmMaster> batch =
                masterRepository.findTop20ByDgftStatusOrderByAddedDateAsc(
                        "Awaiting request initiated");

        if (batch.isEmpty()) {
            return;
        }

        processBatch(batch);
    }

    private void processBatch(List<DgftOrmMaster> batch) {

        String uniqueTxId =
                "TXN-" + UUID.randomUUID().toString().substring(0, 15).toUpperCase();

        DgftOrmMessageMaster messageMaster = new DgftOrmMessageMaster();
        messageMaster.setUniqueTxId(uniqueTxId);
        messageMaster.setStatus("MSG_PUSH_NEW");
        messageMaster.setDgftAckStatus("REQUEST_INITIATED");
        messageMaster.setDgftPushInitTime(LocalDateTime.now());

        try {

            objectMapper.registerModule(new JavaTimeModule());

            String json = objectMapper.writeValueAsString(batch);

            messageMaster.setRequestJsonObj(json);

        } catch (Exception ex) {

            messageMaster.setRequestJsonObj("{}");
        }

        messageMaster = messageMasterRepository.save(messageMaster);

        List<DgftOrmMessageDetail> details = new ArrayList<>();

        for (DgftOrmMaster orm : batch) {

            DgftOrmMessageDetail detail = new DgftOrmMessageDetail();

            detail.setDgftOrmMessageMaster(messageMaster);
            detail.setOrmNumber(orm.getOrmNumber());
            detail.setOrmIssueDate(orm.getOrmIssueDate());
            detail.setStatus("NEW");

            details.add(detail);

            orm.setFlag("P");
            orm.setStatus("ACTIVE");
            orm.setDgftFlag("F");
            orm.setDgftStatus("Request initiated");
            orm.setBankUniqueTransactionId(uniqueTxId);
        }

        messageDetailRepository.saveAll(details);
        masterRepository.saveAll(batch);
    }
}