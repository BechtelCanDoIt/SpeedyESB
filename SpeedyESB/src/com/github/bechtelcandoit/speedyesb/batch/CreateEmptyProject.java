package com.github.bechtelcandoit.speedyesb.batch;

import org.wso2.developerstudio.eclipse.capp.maven.*;

import com.github.bechtelcandoit.speedyesb.ui.mvn.wizard.BatchMvnMultiModuleWizard;

public class CreateEmptyProject {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		//CreateEmptyProject.test();
	}
	
	public void test(){
		
		//DEV Debugging
		//TODO: remove these and get from our custom wizard!
		String groupId = "com.demo.package";
		String artifactId = "DemoArtifact";
		String version = "1.0.0";
		
		new BatchMvnMultiModuleWizard(
				BatchMvnMultiModuleWizard.createModel(groupId, artifactId, version));
	}

}
