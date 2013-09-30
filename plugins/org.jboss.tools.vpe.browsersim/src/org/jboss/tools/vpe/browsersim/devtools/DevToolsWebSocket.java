package org.jboss.tools.vpe.browsersim.devtools;

import java.io.IOException;

import javax.servlet.ServletContext;

import org.eclipse.jetty.websocket.WebSocket;

public class DevToolsWebSocket implements WebSocket.OnTextMessage {

	private Connection connection;
	private ServletContext context;

	public DevToolsWebSocket(ServletContext context) {
		this.context = context;
	}
	
	@Override
	public void onOpen(Connection connection) {
		this.connection = connection;
		if (context.getAttribute("org.jboss.tools.vpe.browsersim.devtools.DevToolsWebSocket") != null) {
			connection.close();
			System.out.println("Another client is already connected. Connection refused.");
		} else {
			context.setAttribute("org.jboss.tools.vpe.browsersim.devtools.DevToolsWebSocket", this);
			System.out.println("Client connected.");
		}
	}

	@Override
	public void onClose(int closeCode, String message) {
		DevToolsWebSocket mainSocket = (DevToolsWebSocket) context.getAttribute("org.jboss.tools.vpe.browsersim.devtools.DevToolsWebSocket");
		if (mainSocket == this) {
			context.removeAttribute("org.jboss.tools.vpe.browsersim.devtools.DevToolsWebSocket");
			System.out.println("Client disconnected.");
		}
	}

	@Override
	public void onMessage(String data) {
		DevToolsDebuggerServer.sendMessageToBrowser(data);
	}

	public void sendMessage(String data) throws IOException  {
		connection.sendMessage(data);
	}
}
