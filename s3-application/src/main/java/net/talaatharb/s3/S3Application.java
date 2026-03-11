package net.talaatharb.s3;

import javafx.application.Application;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class S3Application{
	public static void main(String[] args) {
		log.debug("UI Application Starting");
		Application.launch(JavafxApplication.class, args);
	}
}