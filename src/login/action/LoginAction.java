package login.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import login.form.LoginForm;
import login.model.AccountBean;

public class LoginAction extends Action {
	public ActionForward execute(ActionMapping mapping, ActionForm form, 
		HttpServletRequest request, HttpServletResponse response) {
		LoginForm loginForm = (LoginForm) form;
		
		AccountBean ab = new AccountBean();
		String userName = loginForm.getUserName();
		String password = loginForm.getPassword();
		ab.setUserName(userName);
		ab.setPassword(password);
		
		if(userName.equals("a") && password.equals("a")) {
			System.out.println("success");
			return mapping.findForward("success");
		}
		else {
			System.out.println("fail");
			return mapping.findForward("fail");
		}
	}
}
