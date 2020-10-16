package org.epragati.controller;

import org.epragati.common.service.DeemedAutoActionService;
import org.epragati.exception.BadRequestException;
import org.epragati.util.GateWayResponse;
import org.epragati.util.RequestMappingUrls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RequestMappingUrls.COMMON_DEEMED)
public class DeemedAutoActionContoller {

	private static final Logger logger = LoggerFactory.getLogger(DeemedAutoActionContoller.class);
	
	
	@Autowired
	private DeemedAutoActionService deemedAutoActionService;
	
	@GetMapping(path = "/deemedNewRegSechduler", produces = { MediaType.APPLICATION_JSON_VALUE })
	public GateWayResponse<?> deemedNewRegSechduler() {
		try {
			logger.info("deemed New Reg auto action started using contoller");
			deemedAutoActionService.newRegDeemedAutoAction();
			return new GateWayResponse<>(HttpStatus.OK,"success");
		} catch (BadRequestException e) {
			logger.error("{}", e.getMessage());
			return new GateWayResponse<>(HttpStatus.BAD_REQUEST, e.getMessage());
		} catch (Exception e) {
			logger.error("{}", e);
			return new GateWayResponse<>(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
	
	@GetMapping(path = "/deemedRegServiceSechduler", produces = { MediaType.APPLICATION_JSON_VALUE })
	public GateWayResponse<?> deemedRegServiceSechduler() {
		try {
			logger.info("deemed Reg services auto action started using contoller");
			deemedAutoActionService.regServiceAutoAction();
			return new GateWayResponse<>(HttpStatus.OK,"success");
		} catch (BadRequestException e) {
			logger.error("{}", e.getMessage());
			return new GateWayResponse<>(HttpStatus.BAD_REQUEST, e.getMessage());
		} catch (Exception e) {
			logger.error("{}", e);
			return new GateWayResponse<>(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
}
