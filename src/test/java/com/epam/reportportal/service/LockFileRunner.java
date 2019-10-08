package com.epam.reportportal.service;

import com.epam.reportportal.listeners.ListenerParameters;

import java.io.IOException;
import java.io.InputStreamReader;

public class LockFileRunner {
	public static final String WELCOME_MESSAGE = "Lock ready, press any key to continue...";

	public static void main(String[] args) throws IOException {
		String lockFileName = args[0];
		String syncFileName = args[1];
		String instanceUuid = args[2];

		ListenerParameters params = new ListenerParameters();
		params.setLockFileName(lockFileName);
		params.setSyncFileName(syncFileName);
		LockFile lock = new LockFile(params);
		System.out.println(WELCOME_MESSAGE);
		InputStreamReader isr = new InputStreamReader(System.in);
		isr.read(new char[3]);
		System.out.println(lock.obtainLaunchUuid(instanceUuid));
		isr.read(new char[3]);
		lock.finishInstanceUuid(instanceUuid);
		isr.close();
	}
}
