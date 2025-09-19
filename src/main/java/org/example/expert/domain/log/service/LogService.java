package org.example.expert.domain.log.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.domain.log.entity.Log;
import org.example.expert.domain.log.repository.LogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Getter
@Service
@RequiredArgsConstructor
@Slf4j
public class LogService {
    private final LogRepository logRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(String message) {
        Logger logger = LoggerFactory.getLogger(this.getClass());
        Log log = new Log(message);
        Log savedLog = logRepository.save(log);
        logger.info("저장된 로그 : {}",savedLog.getMessage());
    }
}
