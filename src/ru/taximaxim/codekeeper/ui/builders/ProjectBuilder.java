package ru.taximaxim.codekeeper.ui.builders;

import java.nio.file.Paths;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import ru.taximaxim.codekeeper.ui.Log;
import ru.taximaxim.codekeeper.ui.UIConsts.NATURE;
import ru.taximaxim.codekeeper.ui.pgdbproject.parser.PgDbParser;

public class ProjectBuilder extends IncrementalProjectBuilder {

    @Override
    protected IProject[] build(int kind, Map<String, String> args,
            IProgressMonitor monitor) throws CoreException {
        IProject proj = getProject();
        if (!proj.hasNature(NATURE.ID)) {
            return null;
        }
        final PgDbParser parser = PgDbParser.getParser(proj);
        
        switch (kind) {
        case IncrementalProjectBuilder.AUTO_BUILD:
        case IncrementalProjectBuilder.INCREMENTAL_BUILD:
            IResourceDelta delta = getDelta(getProject());
            try {
                buildIncrement(delta, parser, monitor);
            } catch (CoreException ex) {
                Log.log(Log.LOG_ERROR, "Error processing build delta", ex); //$NON-NLS-1$
            }
            break;
            
        case IncrementalProjectBuilder.FULL_BUILD:
            parser.getObjFromProject(monitor);
            break;
        }
        return new IProject[] { proj };
    }
    
    private void buildIncrement(IResourceDelta delta, final PgDbParser parser,
            IProgressMonitor monitor) throws CoreException {
        final int[] count = new int[1];
        delta.accept(new IResourceDeltaVisitor() {

            @Override
            public boolean visit(IResourceDelta delta) throws CoreException {
                if (delta.getResource() instanceof IFile) {
                    ++count[0];
                }
                return true;
            }
        });
        final SubMonitor sub = SubMonitor.convert(monitor, count[0]);
        
        delta.accept(new IResourceDeltaVisitor() {
            
            @Override
            public boolean visit(IResourceDelta delta) {
                if (!(delta.getResource() instanceof IFile)) {
                    return true;
                }
                sub.worked(1);
                
                switch (delta.getKind()) {
                case IResourceDelta.REMOVED:
                case IResourceDelta.REMOVED_PHANTOM:
                case IResourceDelta.REPLACED:
                    parser.removePathFromRefs(Paths.get(delta.getResource().getLocationURI()));
                    break;
                    
                default:
                    parser.getObjFromProjFile(delta.getResource().getLocationURI());
                    break;
                }
                return true;
            }
        });
        parser.notifyListeners();
    }
}
