package utb.fai;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;


public class SocketHandler {
	/** mySocket je socket, o který se bude tento SocketHandler starat */
	final Socket mySocket;

	/** client ID je øetìzec ve formátu <IP_adresa>:<port> */
	final String clientID;

	/**
	 * activeHandlers je reference na mnoinu vech právì bìících SocketHandlerù.
	 * Potøebujeme si ji udrovat, abychom mohli zprávu od tohoto klienta
	 * poslat vem ostatním!
	 */
	final ActiveHandlers activeHandlers;

	/**
	 * messages je fronta pøíchozích zpráv, kterou musí mít kaý klient svoji
	 * vlastní - pokud bude je pøetíená nebo nefunkèní klientova sí,
	 * èekají zprávy na doruèení právì ve frontì messages
	 */
	final ArrayBlockingQueue<String> messages = new ArrayBlockingQueue<String>(100);

	final Set<String> groups = ConcurrentHashMap.newKeySet();
	/**
	 * startSignal je synchronizaèní závora, která zaøizuje, aby oba tasky
	 * OutputHandler.run() a InputHandler.run() zaèaly ve stejný okamik.
	 */
	final CountDownLatch startSignal = new CountDownLatch(2);

	/** outputHandler.run() se bude starat o OutputStream mého socketu */
	final OutputHandler outputHandler = new OutputHandler();
	/** inputHandler.run() se bude starat o InputStream mého socketu */
	final InputHandler inputHandler = new InputHandler();
	/**
	 * protoe v outputHandleru nedovedu detekovat uzavøení socketu, pomùe mi
	 * inputFinished
	 */
	volatile boolean inputFinished = false;
	volatile String name;

	public SocketHandler(Socket socket, ActiveHandlers registry) {
		mySocket = socket;
		clientID = socket.getInetAddress().toString() + ":" + socket.getPort();
		activeHandlers = registry;
	}

	class OutputHandler implements Runnable {
		@Override public void run() {
			try {
				startSignal.countDown();
				startSignal.await();
				PrintWriter out = new PrintWriter(new OutputStreamWriter(mySocket.getOutputStream(), StandardCharsets.UTF_8), true);
				out.println("You are connected from " + clientID);
				while (true) {
					String message = messages.take();// blokující ètení - pokud není ve frontì zpráv nic, uspi se!
                    if (inputFinished) break;
                    out.println(message);
                    if (out.checkError()) break;
				}
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            }
		}
	}

	class InputHandler implements Runnable {
		@Override public void run() {
			try {
				startSignal.countDown();
				startSignal.await();
				activeHandlers.add(SocketHandler.this);
				BufferedReader reader = new BufferedReader(new InputStreamReader(mySocket.getInputStream(), "UTF-8"));
				String request;
				/**
				 * v okamiku, kdy nás Thread pool spustí, pøidáme se do mnoiny
				 * vech aktivních handlerù, aby chodily zprávy od ostatních i nám
				 */
				while ((request = reader.readLine()) != null) { // pøila od mého klienta nìjaká zpráva?
					// ano - poli ji vem ostatním klientùm
					if (name == null) {
                        if (!activeHandlers.setName(SocketHandler.this, request.trim())) {
                            messages.offer("Name already in use or invalid. Choose another name.");
                        }
                        continue;
                    }
                if (request.startsWith("#setMyName ")) {
                        if (!activeHandlers.setName(SocketHandler.this, request.substring(11).trim())) {
                            messages.offer("Name already in use or invalid.");
                        }
                    } else if (request.startsWith("#sendPrivate ")) {
                        String args = request.substring(13);
                        int separator = args.indexOf(' ');
                        if (separator > 0 && separator < args.length() - 1) {
                            activeHandlers.sendPrivate(SocketHandler.this, args.substring(0, separator), args.substring(separator + 1));
                        }
                    } else if (request.startsWith("#join ")) {
                        activeHandlers.join(SocketHandler.this, request.substring(6).trim());
                    } else if (request.startsWith("#leave ")) {
                        activeHandlers.leave(SocketHandler.this, request.substring(7).trim());
                    } else if (request.equals("#groups")) {
                        messages.offer(activeHandlers.groups(SocketHandler.this));
                    } else if (!request.startsWith("#")) {
                        activeHandlers.sendMessageToAll(SocketHandler.this, request);
                    }
                }
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            } finally {
                activeHandlers.remove(SocketHandler.this);
                inputFinished = true;
                messages.offer(""); // Wake output thread if it is waiting.
                try { mySocket.close(); } catch (IOException ignored) { }
            }
		}

	}
}
