 
package ru.taximaxim.codekeeper.ui.handlers;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.swt.program.Program;

public class OpenLog {
    
	@Execute
	public void execute() {
        Program.launch(Platform.getLogFileLocation().toFile().getAbsolutePath());
	}
}