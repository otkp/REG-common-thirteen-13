package org.epragati.controller;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.epragati.exception.BadRequestException;
import org.epragati.util.GateWayResponse;
import org.epragati.util.JwtAuthenticationRequest;
import org.epragati.util.RequestMappingUrls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@RestController
@RequestMapping(RequestMappingUrls.COMMON_AUTHENTICATION)
public class AuthenticationController {
	private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

	private static LoadingCache<String, String> capchaMap;
	private static Font font;
	private static BufferedImage cpimg;

	@PostConstruct
	private void init() {
		capchaMap = CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(2, TimeUnit.MINUTES)
				.build(new CacheLoader<String, String>() {

					@Override
					public String load(String capchaValue) throws Exception {
						return StringUtils.EMPTY;

					}
				});
		font = new Font("Arial", Font.BOLD, 20);
		int width = 130;
		int height = 40;
		cpimg = new BufferedImage(width, height, BufferedImage.OPAQUE);
	}

	private String generateCaptchaTextMethod2(int captchaLength) {

		String saltChars = "1234567890";
		StringBuffer captchaStrBuffer = new StringBuffer();
		java.util.Random rnd = new java.util.Random();

		// build a random captchaLength chars salt
		while (captchaStrBuffer.length() < captchaLength) {
			int index = (int) (rnd.nextFloat() * saltChars.length());
			captchaStrBuffer.append(saltChars.substring(index, index + 1));
		}

		return captchaStrBuffer.toString();

	}

	@RequestMapping(value = "/generateCaptcha", method = RequestMethod.GET, produces = {
			MediaType.APPLICATION_JSON_VALUE })
	@ResponseBody
	public GateWayResponse<?> generateCaptcha(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		final String FILE_TYPE = "jpeg";
		String captchaStr = "";

		captchaStr = generateCaptchaTextMethod2(6);
		String capchaID = UUID.randomUUID().toString();
		capchaMap.put(capchaID, captchaStr);

		try {

			int width = 130;
			Color bg = Color.WHITE;
			int height = 40;
			//Color fg = new Color(0, 100, 0);
			Color fg =new Color(255, 153, 0);

			//Font font = new Font("Arial", Font.BOLD, 20);
			Font font = new Font("Georgia", Font.BOLD, 18);
			//"Georgia"
			//BufferedImage cpimg = new BufferedImage(width, height, BufferedImage.OPAQUE);
			BufferedImage cpimg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			//Graphics g = cpimg.createGraphics();
			Graphics2D  g = cpimg.createGraphics();
			g.setFont(font);
			RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	        rh.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	        g.setRenderingHints(rh);
	        GradientPaint gp = new GradientPaint(0, 0, Color.red, 0, height / 2, Color.black, true);
			/*
			 * g.setPaint(gp);  g.setColor(bg); 
			 */
	        g.setPaint(gp);
	        g.fillRect(0, 0, width, height);
			g.setColor(fg);
			g.drawString(captchaStr, 10, 25);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			ImageIO.write(cpimg, FILE_TYPE, baos);
			byte[] imgBytes = baos.toByteArray();

			IOUtils.closeQuietly(baos);
			JwtAuthenticationRequest userVO = new JwtAuthenticationRequest();
			userVO.setCapchaId(capchaID);
			userVO.setCapchaEncodedImg(Base64.getEncoder().encodeToString(imgBytes));

			return new GateWayResponse<>(userVO);

		} catch (Exception e) {
			throw new BadRequestException("OOps.. There is an Error while generate capcha. please try again.");
		}

	}

	@RequestMapping(value = "/validateCaptcha", method = RequestMethod.POST, produces = {
			MediaType.APPLICATION_JSON_VALUE })
	private GateWayResponse<?> validateCaptcha(@RequestBody JwtAuthenticationRequest userVO) {

		try {
			if (!(StringUtils.isNoneBlank(userVO.getCapchaId()) && StringUtils.isNoneBlank(userVO.getCapchaValue())
					&& userVO.getCapchaValue().equals(capchaMap.get(userVO.getCapchaId())))) {
				return new GateWayResponse<>(false);
			}
		} catch (ExecutionException e) {
			logger.error("Exception while validating Captcha in common [{}]", e);
		}
		return new GateWayResponse<>(true);

	}

}
