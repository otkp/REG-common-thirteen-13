package org.epragati.controller;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.epragati.common.service.CommonService;
import org.epragati.common.service.NumberGenerationService;
import org.epragati.common.service.impl.SyncroServiceFactory;
import org.epragati.constants.CommonConstants;
import org.epragati.constants.MessageKeys;
import org.epragati.exception.BadRequestException;
import org.epragati.jwt.JwtUser;
import org.epragati.master.dto.StagingRegistrationDetailsDTO;
import org.epragati.master.service.StagingRegistrationDetailsSerivce;
import org.epragati.payments.service.PaymentGateWayService;
import org.epragati.payments.vo.TransactionDetailVO;
import org.epragati.rta.vo.PrGenerationVO;
import org.epragati.tax.service.TaxService;
import org.epragati.util.GateWayResponse;
import org.epragati.util.IPWebUtils;
import org.epragati.util.RequestMappingUrls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RequestMappingUrls.SERVICES_PAYMENT)
public class RegPaymentGateWayController {

	private static final Logger logger = LoggerFactory.getLogger(RegPaymentGateWayController.class);

	@Autowired
	private PaymentGateWayService paymentGatewayService;

	@Autowired
	private TaxService taxService;

	@Autowired
	private StagingRegistrationDetailsSerivce stagingRegistrationDetailsSerivce;

	@Autowired
	private SyncroServiceFactory syncroServiceFactory;
	
	@Autowired
	private NumberGenerationService numberGenerationService;
	
	@Value("${isTestSchedulers:false}")
	private boolean isTest;
	
	@Autowired
	private IPWebUtils webUtils;

	@PostMapping(path = "/regPaymentRequestObjectDealer", produces = { MediaType.APPLICATION_JSON_VALUE })
	public GateWayResponse<?> getPaymentRequestObject(@RequestBody TransactionDetailVO transactionDetailVO) {
		StagingRegistrationDetailsDTO stagingDetails = null;
		try {
			final String appNo = transactionDetailVO.getFormNumber();

			synchronized (appNo.intern()) {

				Optional<StagingRegistrationDetailsDTO> registrationDetails = stagingRegistrationDetailsSerivce
						.FindbBasedOnApplicationNo(transactionDetailVO.getFormNumber());

				if (registrationDetails.isPresent()) {
					stagingDetails = registrationDetails.get();
					if (stagingDetails.getSpecialNumberRequired() && stagingDetails.getOfficeDetails() != null
							&& stagingDetails.getOfficeDetails().getOfficeCode()
									.equalsIgnoreCase(CommonConstants.OTHER)) {
						return new GateWayResponse<>(HttpStatus.BAD_REQUEST, "Special Number Not Eligible");
					}
					paymentGatewayService.verifyPaymentStatus(stagingDetails.getApplicationNo(),
							stagingDetails.getPaymentTransactionNo());

					paymentGatewayService.paymentIntiationVerification(stagingDetails.getApplicationStatus(),
							stagingDetails.getApplicationNo());

					stagingDetails = taxService.getTaxDetails(stagingDetails);
					paymentGatewayService.taxIntegration(stagingDetails, transactionDetailVO);

					transactionDetailVO = paymentGatewayService.getTransactionDetailsForPayments(stagingDetails,
							transactionDetailVO);
					@SuppressWarnings("unchecked")
					Optional<TransactionDetailVO> optinalModel = (Optional<TransactionDetailVO>) paymentGatewayService
							.prepareRequestObject(transactionDetailVO, Optional.of(stagingDetails), "STAGING");
					optinalModel.get().setActualAmount(optinalModel.get().getAmount().toString());
					if (optinalModel.isPresent()) {
						logger.info(optinalModel.get().getActualAmount());
						return new GateWayResponse<>(HttpStatus.OK, optinalModel.get(), null);
					}

				} else {
					logger.info("Application Number Not Found: [{}]", transactionDetailVO.getFormNumber());
					return new GateWayResponse<>(false, HttpStatus.ACCEPTED,
							"Application Number Not Found: " + transactionDetailVO.getFormNumber());
				}

			}
			return new GateWayResponse<>(false, HttpStatus.OK, "Unable to prepare the payment request",
					Collections.emptyList());
		} catch (Exception e) {
			logger.error("{}", e);
			return new GateWayResponse<>(false, HttpStatus.OK, e.getMessage());
		}
	}

	@PostMapping(path = "/generatePrNo", produces = { MediaType.APPLICATION_JSON_VALUE })
	public GateWayResponse<?> getGeneratedPrNo(@RequestBody PrGenerationVO prGenVO) {
		try {
			if (StringUtils.isEmpty(prGenVO.getApplicationNo())) {
				throw new BadRequestException("ApplicationNo not found");
			}
			logger.info("generatePrNo service successfully initiated ");
			CommonService commonService = syncroServiceFactory.getCommonServiceInst();
			synchronized(prGenVO.getApplicationNo().intern()){
				logger.info("sync block initiated for generate pr ");
				return new GateWayResponse<>(true, HttpStatus.OK, commonService.geneatePrNo(prGenVO));
			}
		} catch (BadRequestException e) {
			logger.error("{}", e.getMessage());
			return new GateWayResponse<>(HttpStatus.BAD_REQUEST, e.getMessage());
		} catch (Exception e) {
			logger.error("{}", e);
			return new GateWayResponse<>(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@GetMapping(path = "/generatePaymentReciept", produces = { MediaType.APPLICATION_JSON_VALUE })
	public GateWayResponse<?> generatePaymentReciept() {
		try {
			String recieptNo = syncroServiceFactory.generatePaymentReciept();
			return new GateWayResponse<>(true, HttpStatus.OK, recieptNo);
		} catch (BadRequestException e) {
			logger.error("{}", e.getMessage());
			return new GateWayResponse<>(HttpStatus.BAD_REQUEST, e.getMessage());
		} catch (Exception e) {
			logger.error("{}", e);
			return new GateWayResponse<>(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
	
	@GetMapping(path = "/generateTRSeries", produces = { MediaType.APPLICATION_JSON_VALUE })
	public GateWayResponse<?> generateTrNumber(@RequestParam Integer trDistrictId) {
		try {
			String trNumber = numberGenerationService.getTrGeneratedSeries(trDistrictId);
			return new GateWayResponse<>(true, HttpStatus.OK, trNumber);
		} catch (BadRequestException e) {
			logger.error("{}", e.getMessage());
			return new GateWayResponse<>(HttpStatus.BAD_REQUEST, e.getMessage());
		} catch (Exception e) {
			logger.error("{}", e);
			return new GateWayResponse<>(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
	
	/*
	 * @GetMapping(path = "/testRamdomList", produces = {
	 * MediaType.APPLICATION_JSON_VALUE }) public GateWayResponse<?>
	 * testRamdomList() { try { syncroServiceFactory.testRandom(); return new
	 * GateWayResponse<>(true, HttpStatus.OK,"success"); } catch
	 * (BadRequestException e) { logger.error("{}", e.getMessage()); return new
	 * GateWayResponse<>(HttpStatus.BAD_REQUEST, e.getMessage()); } catch (Exception
	 * e) { logger.error("{}", e); return new
	 * GateWayResponse<>(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()); } }
	 */
	@GetMapping(path = "/getGreetingMessage", produces = { MediaType.APPLICATION_JSON_VALUE })
	public GateWayResponse<?> getGreetingMessage() {
		try {
			return new GateWayResponse<String>(HttpStatus.OK, "Hi HAVE A NICE DAY!");
		} catch (BadRequestException e) {
			logger.error("{}", e.getMessage());
			return new GateWayResponse<>(HttpStatus.BAD_REQUEST, e.getMessage());
		} catch (Exception e) {
			logger.error("{}", e);
			return new GateWayResponse<>(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
	
	@GetMapping(value = "prGenerationSechduler", produces = { MediaType.APPLICATION_JSON_VALUE })
	public GateWayResponse<?> prGenerationScheduler(@RequestHeader("Authorization") String token,HttpServletRequest request) {
			try {
				if(!isTest) {
					numberGenerationService.validateRequest(token, webUtils.getClientIp(request));
				}
				numberGenerationService.prGenerationScheduler();
			} catch (Exception ex) {
				logger.error("Exception while run prGenerationSechduler", ex.getMessage());
				return new GateWayResponse<>(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
			}
			return new GateWayResponse<>(HttpStatus.OK, "success");
		
	}
	@GetMapping(value = "caluclateRegistrationCount", produces = { MediaType.APPLICATION_JSON_VALUE,
			MediaType.APPLICATION_XML_VALUE })
	public GateWayResponse<?> caluclateRegistrationCount(@RequestHeader("Authorization") String token
			,HttpServletRequest request,
			@RequestParam(value = "countDate", required=false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate countDate
			) {
		try {
			if(!isTest) {
				numberGenerationService.validateRequest(token, webUtils.getClientIp(request));
			}
			numberGenerationService.caluclateRegistrationCount(countDate);
		} catch (Exception ex) {
			logger.error("Exception while run prGenerationSechduler", ex);
			return new GateWayResponse<>(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
		}
		return new GateWayResponse<>(HttpStatus.OK, "success");
	}
}
