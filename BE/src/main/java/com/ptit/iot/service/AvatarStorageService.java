package com.ptit.iot.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import javax.imageio.ImageIO;

import com.ptit.iot.exception.BusinessException;
import com.ptit.iot.exception.DomainCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AvatarStorageService {
	private static final long MAX_FILE_SIZE = 20 * 1024 * 1024;
	private static final int MAX_IMAGE_DIMENSION = 10000;
	private static final int AVATAR_DIMENSION = 512;
	private final Path avatarDirectory;

	public AvatarStorageService(@Value("${app.storage.avatar-directory:uploads/avatars}") String directory) {
		this.avatarDirectory = Path.of(directory).toAbsolutePath().normalize();
	}

	public String save(MultipartFile file) {
		if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
			throw new BusinessException(DomainCode.INVALID_AVATAR);
		}
		String contentType = file.getContentType();
		if (!"image/jpeg".equalsIgnoreCase(contentType) && !"image/png".equalsIgnoreCase(contentType)) {
			throw new BusinessException(DomainCode.INVALID_AVATAR);
		}

		BufferedImage source;
		try (InputStream input = file.getInputStream()) {
			source = ImageIO.read(input);
		} catch (IOException exception) {
			throw new BusinessException(DomainCode.INVALID_AVATAR);
		}
		if (source == null || source.getWidth() > MAX_IMAGE_DIMENSION || source.getHeight() > MAX_IMAGE_DIMENSION) {
			throw new BusinessException(DomainCode.INVALID_AVATAR);
		}

		try {
			BufferedImage avatar = resize(source);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			ImageIO.write(avatar, "png", output);
			Files.createDirectories(avatarDirectory);
			String filename = UUID.randomUUID() + ".png";
			Files.write(avatarDirectory.resolve(filename), output.toByteArray());
			return "/uploads/avatars/" + filename;
		} catch (IOException exception) {
			throw new IllegalStateException("Could not store avatar image", exception);
		}
	}

	public String resourceLocation() {
		String location = avatarDirectory.toUri().toString();
		return location.endsWith("/") ? location : location + "/";
	}

	private BufferedImage resize(BufferedImage source) {
		double scale = Math.min(1.0, (double) AVATAR_DIMENSION / Math.max(source.getWidth(), source.getHeight()));
		int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
		int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
		BufferedImage result = new BufferedImage(width, height,
				source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = result.createGraphics();
		try {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics.drawImage(source, 0, 0, width, height, null);
		} finally {
			graphics.dispose();
		}
		return result;
	}
}
