/**
 * 
 */
package com.github.bechtelcandoit.speedyesb.ui.mvn.wizard;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.wso2.developerstudio.eclipse.logging.core.IDeveloperStudioLog;
import org.wso2.developerstudio.eclipse.logging.core.Logger;
import org.wso2.developerstudio.eclipse.maven.util.MavenUtils;
import org.wso2.developerstudio.eclipse.platform.core.exception.ObserverFailedException;
import org.wso2.developerstudio.eclipse.platform.core.utils.Constants;
import org.wso2.developerstudio.eclipse.platform.ui.Activator;
import org.wso2.developerstudio.eclipse.platform.ui.mvn.wizard.MvnMultiModuleModel;
import org.wso2.developerstudio.eclipse.platform.ui.mvn.wizard.MvnMultiModuleWizard;
import org.wso2.developerstudio.eclipse.utils.file.FileUtils;
import org.wso2.developerstudio.eclipse.utils.project.ProjectUtils;

/**
 * @author bro_sbechtel
 *
 */
public class BatchMvnMultiModuleWizard extends MvnMultiModuleWizard {
	
	private static IDeveloperStudioLog log = Logger.getLog(Activator.PLUGIN_ID);

	protected static final String MAVEN_ECLIPSE_PLUGIN = "org.apache.maven.plugins:maven-eclipse-plugin:2.9";

	//These overlay the private variables with the same name in the super class.
	protected MvnMultiModuleModel moduleModel;
	protected IProject project;
	protected IProject multiModuleProject;
	
	/**
	 * disable default constructor
	 */
	private BatchMvnMultiModuleWizard() {
		//do nothing
	}

	/**
	 * Provides a way to set up the model externally for streamlined settings
	 * and batch processing.
	 * 
	 * @param moduleModel
	 */
	public BatchMvnMultiModuleWizard(MvnMultiModuleModel moduleModel) {
		this.moduleModel = new MvnMultiModuleModel();
		setModel(this.moduleModel);
		this.init();
		this.performFinish(this.moduleModel);
	}

	/**
	 * @throws ObserverFailedException
	 */
	static public MvnMultiModuleModel createModel(String groupId, String artifactId,
			String version) {
		MvnMultiModuleModel moduleModel = new MvnMultiModuleModel();
		try {

			moduleModel.setModelPropertyValue("group.id", groupId);
			moduleModel.setModelPropertyValue("project.name", artifactId);
			moduleModel.setModelPropertyValue("version.id", version);

		} catch (ObserverFailedException e) {
			log.error(
					"Error occured while trying to inject values to the Project Model",
					e);
		}
		return moduleModel;
	}

	public boolean performFinish(MvnMultiModuleModel moduleModel) {
		// If the multiModuleProject is not empty, then this is thru UI. Just generate the POM
		MavenProject mavenProject =
		                            MavenUtils.createMavenProject(moduleModel.getGroupId(),
		                                                          moduleModel.getArtifactId(),
		                                                          moduleModel.getVersion(), "pom");
		
		if(moduleModel.isRequiredParent()){
			Parent parent = new Parent();
			parent.setArtifactId(moduleModel.getParentArtifact());
			parent.setGroupId(moduleModel.getParentGroup());
			parent.setVersion(moduleModel.getParentVersion());
			String relativePath = moduleModel.getRelativePath();
			if(relativePath!=null && !relativePath.trim().isEmpty()){
				parent.setRelativePath(relativePath);
			}
			mavenProject.getModel().setParent(parent);
		} else{
			mavenProject.getModel().setParent(null);
		}

		List<String> modules = mavenProject.getModules();

		List<IProject> selectedProjects = moduleModel.getSelectedProjects();
		
		selectedProjects=sortProjects(selectedProjects);

		if (multiModuleProject != null) {
			IFile pomFile = multiModuleProject.getFile("pom.xml");
			if (pomFile.exists()) {
				// Parse the pom and see the packaging type
				try {
					MavenProject mavenProject2 =
					                             MavenUtils.getMavenProject(pomFile.getLocation()
					                                                               .toFile());
					String packaging = mavenProject2.getPackaging();
					if (!"pom".equalsIgnoreCase(packaging)) {
						addMavenModules(multiModuleProject, mavenProject, modules,
						                selectedProjects, pomFile);
					} else {
						modules = mavenProject2.getModules();
						mavenProject2.setGroupId(moduleModel.getGroupId());
						mavenProject2.setArtifactId(moduleModel.getArtifactId());
						mavenProject2.setVersion(moduleModel.getVersion());
						mavenProject2.getModel().setParent(mavenProject.getModel().getParent());
						addMavenModules(multiModuleProject, mavenProject2, modules,
						                selectedProjects, pomFile);
					}

				} catch (Exception e) {
					log.error("Error occured while trying to generate the Maven Project for the Project Pom",
					          e);
				}

			} else {
				// Since pom is not there, just create the new pom with all the necessary things
				addMavenModules(multiModuleProject, mavenProject, modules, selectedProjects,
				                pomFile);
			}
//			Adding Maven Multi Module Nature to POM generated Project 
			addMavenMultiModuleProjectNature(multiModuleProject);

		} else {
			try {
				moduleModel.setProjectName(moduleModel.getArtifactId());
				project = createNewProject();
				
				addMavenMultiModuleProjectNature(project);

				addMavenModules(project, mavenProject, modules, selectedProjects,
				                project.getFile("pom.xml"));

			} catch (CoreException e) {
				log.error("Error occured while creating the new Maven Multi Module Project", e);
			} catch (ObserverFailedException e) {
				log.error("Error occured while trying to inject values to the Project Model", e);
			}
		}
		return true;
	}
	
	/**
	 * Copied up to add visibiltiy for this class.
	 * @param projectToAdddNature
	 */
	protected void addMavenMultiModuleProjectNature(IProject projectToAdddNature){
		try {
			ProjectUtils.addNatureToProject(projectToAdddNature, false,Constants.MAVEN_MULTI_MODULE_PROJECT_NATURE);
			projectToAdddNature.refreshLocal(IResource.DEPTH_INFINITE,new NullProgressMonitor());
		} catch (CoreException e) {
			log.error("Error occured while adding the Maven Multi Module Nature to Project", e);
		}
	}
	
	/**
	 * Copied up to add visibility for this class.
	 * @param mavenProject
	 * @param modules
	 * @param selectedProjects
	 * @param pomFile
	 */
	protected void addMavenModules(IProject selectedProject, MavenProject mavenProject, List modules,
	                             List<IProject> selectedProjects, IFile pomFile) {
		modules.clear();
		for (IProject iProject : selectedProjects) {
			String relativePath =
			                      FileUtils.getRelativePath(selectedProject.getLocation().toFile(),
			                                                iProject.getLocation().toFile()).replaceAll(Pattern.quote(File.separator), "/");
			if (!modules.contains(relativePath)) {
				modules.add(relativePath);
			}
		}
		
		try {
			MavenUtils.saveMavenProject(mavenProject, pomFile.getLocation().toFile());
			selectedProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		} catch (Exception e) {
			log.error("Error occured while trying to save the maven project", e);
		}
		
		try {
			MavenProject mproject = MavenUtils.getMavenProject(pomFile
					.getLocation().toFile());
			List<Plugin> buildPlugins = mproject.getBuildPlugins();
			if (buildPlugins.isEmpty()) {
				MavenUtils.updateWithMavenEclipsePlugin(pomFile.getLocation()
						.toFile(), new String[] {},
						new String[] { Constants.MAVEN_MULTI_MODULE_PROJECT_NATURE });
			} else {
				for (Plugin plugin : buildPlugins) {
					if (MAVEN_ECLIPSE_PLUGIN.equals(plugin.getId())) {
						break;// Since plugin is already in the pom no need to
								// add it again
					} else {
						MavenUtils
								.updateWithMavenEclipsePlugin(
										pomFile.getLocation().toFile(),
										new String[] {},
										new String[] { Constants.MAVEN_MULTI_MODULE_PROJECT_NATURE });
					}
				}
			}
			selectedProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		} catch (Exception e) {
			log.error("Error occured while trying to update the maven project with Eclipse Maven plugin.", e);
		}
	}
}
