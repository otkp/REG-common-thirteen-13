package org.epragati.scheduler;

import java.time.LocalDateTime;

import org.epragati.auditService.AuditLogsService;
import org.epragati.common.service.DeemedAutoActionService;
import org.epragati.common.service.NumberGenerationService;
import org.epragati.constants.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Scheduler {
	
	private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

	@Autowired
	private NumberGenerationService numberGenerationService;
	
	@Autowired
	private AuditLogsService auditLogsService;
	
	@Value("${pr.generation.scheduler.isEnable:false}")
	private Boolean isEnablePRGeneration;

	@Autowired
	private DeemedAutoActionService deemedAutoActionService;
	
	@Value("${auto.action.reg.services.scheduler.isEnable:false}")
	private Boolean isEnableAutoActionRegServices;

	@Value("${auto.action.new.reg.scheduler.isEnable:false}")
	private Boolean isEnableAutoActionNewReg;
	@Value("${registration.count.scheduler.isEnable:false}")
	private Boolean isEnableregCount;
	
	@Scheduled(cron = "${pr.generation.scheduler}")  
	public void prGenerationScheduler() {
		LocalDateTime startTime = LocalDateTime.now();
		LocalDateTime endTime = null;
		String error = null;
		Boolean isExecuteSucess = true;
		if (isEnablePRGeneration) {
			logger.info("New Reg prNo generation schedular starting at time {}", LocalDateTime.now());

			try {
				numberGenerationService.prGenerationScheduler();
			} catch (Exception ex) {
				error = ex.getMessage();
				isExecuteSucess = false;
				logger.error("Exception occured while running New Reg prNo generation  {} ", error);
			}
			endTime = LocalDateTime.now();
			logger.info("New Reg prNo generation schedular end at time {}", endTime);
		}
		else {
			endTime = LocalDateTime.now();
			isExecuteSucess = false;
			logger.info("New Reg prNo generation schedular skip at time {}", endTime);
		}
		auditLogsService.saveScedhuleLogs(Schedulers.PRNUMBERGENERATION, startTime, endTime, isExecuteSucess, error,
				null);
	}
	
	@Scheduled(cron = "${auto.action.reg.services.scheduler}")  
	public void regServicesAutoActionScheduler() {
		LocalDateTime startTime = LocalDateTime.now();
		LocalDateTime endTime = null;
		String error = null;
		Boolean isExecuteSucess = true;
		if (isEnableAutoActionRegServices) {
			logger.info("Reg services deemed action schedular starting at time {} :", LocalDateTime.now());

			try {
				deemedAutoActionService.regServiceAutoAction();
			} catch (Exception ex) {
				error = ex.getMessage();
				isExecuteSucess = false;
				logger.error("Exception occured while running Reg services deemed action {} :", error);
			}
			endTime = LocalDateTime.now();
			logger.info("Reg services deemed action schedular end at time {} :", endTime);
		}
		else {
			endTime = LocalDateTime.now();
			isExecuteSucess = false;
			logger.info("Reg services deemed action schedular skip at time {} :", endTime);
		}
		auditLogsService.saveScedhuleLogs(Schedulers.REGSERAUTOACTION, startTime, endTime, isExecuteSucess, error,
				null);
	}
	
	
	@Scheduled(cron = "${auto.action.new.reg.scheduler}")  
	public void newRegAutoActionScheduler() {
		LocalDateTime startTime = LocalDateTime.now();
		LocalDateTime endTime = null;
		String error = null;
		Boolean isExecuteSucess = true;
		if (isEnableAutoActionNewReg) {
			logger.info("New Reg auto action schedular starting at time {} :", LocalDateTime.now());

			try {
				deemedAutoActionService.newRegDeemedAutoAction();
				} catch (Exception ex) {
				error = ex.getMessage();
				isExecuteSucess = false;
				logger.error("Exception occured while running New Reg auto action  {} :", error);
			}
			endTime = LocalDateTime.now();
			logger.info("New Reg auto action schedular end at time {} :", endTime);
		}
		else {
			endTime = LocalDateTime.now();
			isExecuteSucess = false;
			logger.info("New Reg auto action schedular skip at time {} :", endTime);
		}
		auditLogsService.saveScedhuleLogs(Schedulers.NEWREGAUTOACTION, startTime, endTime, isExecuteSucess, error,
				null);
	}
	
	@Scheduled(cron = "${registration.count.scheduler}")  
	public void registrationCountScheduler() {
		LocalDateTime startTime = LocalDateTime.now();
		LocalDateTime endTime = null;
		String error = null;
		Boolean isExecuteSucess = true;
		if (isEnableregCount) {
			logger.info("RegCount schedular starting at time {}", LocalDateTime.now());

			try {
				numberGenerationService.caluclateRegistrationCount(null);
			} catch (Exception ex) {
				error = ex.getMessage();
				isExecuteSucess = false;
				logger.error("Exception occured while running RegCount Scheduler  {} ", error);
			}
			endTime = LocalDateTime.now();
			logger.info("RegCount schedular end at time {}", endTime);
		}
		else {
			endTime = LocalDateTime.now();
			isExecuteSucess = false;
			logger.info("RegCount schedular skip at time {}", endTime);
		}
		auditLogsService.saveScedhuleLogs(Schedulers.REGISTRATIONCOUNT, startTime, endTime, isExecuteSucess, error,
				null);
	}
}
