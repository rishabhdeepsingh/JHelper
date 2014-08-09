package name.admitriev.jhelper.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import name.admitriev.jhelper.JHelperException;
import name.admitriev.jhelper.components.Configurator;
import name.admitriev.jhelper.generation.IncludesProcessor;
import net.egork.chelper.util.OutputWriter;

import java.io.IOException;


public class GenerateCodeAction extends AnAction {
	@Override
	public void actionPerformed(AnActionEvent e) {
		PsiFile file = e.getData(CommonDataKeys.PSI_FILE);

		if(file == null) {
			System.err.println("file is null");
			return;
		}

		if(!isCppFile(file)) {
			System.err.println("Not a cpp file");
			return;
		}

		Project project = e.getProject();
		if(project == null) {
			throw new JHelperException("no project found");
		}

		VirtualFile outputFile = findFileInProject(project, "output/main.cpp");
		if(outputFile == null) {
			throw new JHelperException("no output file found.");
		}
		String result = IncludesProcessor.process(file);
		writeToFile(outputFile, authorComment(project), result);
		System.err.println("here");
		PsiFile psiOutputFile = PsiManager.getInstance(project).findFile(outputFile);
		if(psiOutputFile == null) {
			throw new JHelperException("can't open output file as PSI");
		}
	}

	private void writeToFile(final VirtualFile outputFile, final String... strings) {
		ApplicationManager.getApplication().runWriteAction(new Runnable() {
			@Override
			public void run() {
				try {
					OutputWriter writer = new OutputWriter(outputFile.getOutputStream(this));
					for (String string : strings) {
						writer.print(string);
					}
					writer.flush();
					writer.close();
				} catch (IOException e) {
					throw new JHelperException("Can't write to output file", e);
				}
			}
		});
	}

	private static String authorComment(Project project) {
		Configurator configurator = project.getComponent(Configurator.class);
		Configurator.State configuration = configurator.getState();

		StringBuilder sb = new StringBuilder();
		sb.append("/**\n");
		sb.append(" * code generated by JHelper\n");
		sb.append(" * @author ").append(configuration.getAuthor()).append('\n');
		sb.append(" */\n\n");
		return sb.toString();
	}

	private static VirtualFile findFileInProject(Project project, String path) {
		VirtualFile projectDirectory = project.getBaseDir();
		return projectDirectory.findFileByRelativePath(path);
	}

	private static boolean isCppFile(PsiFile file) {
		return file.getName().endsWith(".cpp");
	}

}
