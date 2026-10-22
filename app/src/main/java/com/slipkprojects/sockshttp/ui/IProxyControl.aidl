package com.slipkprojects.sockshttp.ui;

interface IProxyControl {
	boolean start();
	
	boolean stop();
	
	boolean isRunning();
	
	int getPort();
}