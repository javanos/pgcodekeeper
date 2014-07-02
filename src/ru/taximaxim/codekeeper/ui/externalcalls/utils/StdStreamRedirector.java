
package ru.taximaxim.codekeeper.ui.externalcalls.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import ru.taximaxim.codekeeper.ui.Log;
import ru.taximaxim.codekeeper.ui.localizations.Messages;
import ru.taximaxim.codekeeper.ui.parts.Console;

/**
 * A Runnable that consumes everything from the {@link InputStream},
 * redirects data to {@link Console} and stores it as a String.
 * 
 * @author Alexander Levsha
 */
public class StdStreamRedirector implements Runnable {

    private BufferedReader in;
    
    private StringBuilder storage = new StringBuilder(2000);
    
    private AtomicBoolean isDestroyed = new AtomicBoolean(false);
    
    /**
     * @param in {@link InputStream} to 
     */
    private StdStreamRedirector(InputStream in) {
        this.in = new BufferedReader(new InputStreamReader(in));
    }
    
    @Override
    public void run() {
        String line = null;
        try {
            while((line = in.readLine()) != null) {
                Console.addMessage(line);
                storage.append(line);
                storage.append(System.lineSeparator());
            }
        } catch(IOException ex) {
            if (isDestroyed.get()) {
                // the process was destroyed by us, exit silently
                return;
            }
            throw new IllegalStateException(
                    Messages.StdStreamRedirector_error_while_reading_from_stdout_stderr, ex);
        }
    }
    
    /**
     * Launches a process combining stdout & stderr and redirecting them
     * onto {@link Console}. Blocks until process exits and all output is consumed.
     * 
     * @param pb process to start 
     * @return captured stdout & stderr output
     * @throws IOException
     */
    public static String launchAndRedirect(ProcessBuilder pb)
            throws IOException {
        StringBuilder sb = new StringBuilder(1000 * pb.command().size());
        for(String param : pb.command()) {
            sb.append(param);
            sb.append(' ');
        }
        String cmd = sb.toString();
        Console.addMessage(cmd);
        
        pb.redirectErrorStream(true);
        final Process p = pb.start();
        
        final StdStreamRedirector redirector = new StdStreamRedirector(p.getInputStream());
        try (BufferedReader t = redirector.in) {
            Thread redirectorThread = new Thread(redirector);
            final AtomicReference<Throwable> lastException = new AtomicReference<>();
            redirectorThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    lastException.set(e);
                    redirector.isDestroyed.set(true);
                    p.destroy();
                }
            });
            redirectorThread.start();
            
            try {
                p.waitFor();
            } catch (InterruptedException ex) {
                redirector.isDestroyed.set(true);
                p.destroy();
            }
            
            try {
                redirectorThread.join();
            } catch (InterruptedException ex) {
                throw new IllegalStateException(Messages.StdStreamRedirector_interrrupted_wait_on_redirector_thread, ex);
            }

            if (!redirector.isDestroyed.get() && p.exitValue() != 0) {
                throw new IOException(Messages.StdStreamRedirector_process_returned_with_error
                            + p.exitValue());
            }
            
            if (lastException.get() != null){
                throw new IOException(Messages.StdStreamRedirector_exception_thrown_while_external_command_output, 
                        lastException.get());
            }
            return redirector.storage.toString();
        } finally {
            StringBuilder msg = new StringBuilder(
                    cmd.length() + redirector.storage.length() + 128);
            msg.append(Messages.StdStreamRedirector_external_command)
                .append(System.lineSeparator())
                .append(cmd)
                .append(System.lineSeparator())
                .append(Messages.StdStreamRedirector_output)
                .append(System.lineSeparator())
                .append(redirector.storage);
            
            Log.log(Log.LOG_INFO, msg.toString());
        }
    }
}