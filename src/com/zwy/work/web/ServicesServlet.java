package com.zwy.work.web;

import com.zwy.work.dao.AccountDao;
import com.zwy.work.dao.BillDao;
import com.zwy.work.dao.CostDao;
import com.zwy.work.dao.ServiceDao;
import com.zwy.work.entity.Account;
import com.zwy.work.entity.Cost;
import com.zwy.work.entity.Service;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServicesServlet extends HttpServlet {
    private int singlePageLimit = 5;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String path = req.getAttribute("path").toString();
        //根据规范做出判断处理
        System.out.println(path);
        switch (path) {
            case "/findServices.do":
                findServices(req, res);
                break;
            case "/toAddService.do":
                toAddService(req, res);
                break;
            case "/addService.do":
                addService(req, res);
                break;
            case "/toModifyService.do":
                toModifyService(req, res);
                break;
            case "/modifyService.do":
                modifyService(req, res);
                break;
            case "/deleteService.do":
                deleteService(req, res);
                break;
            case "/searchServices.do":
                searchServices(req, res);
                break;
            case "/serviceDetail.do":
                serviceDetailById(req, res);
                break;
            case "/setServiceState.do":
                setServiceState(req, res);
                break;
            default:
                throw new RuntimeException("查无此页面");
        }
    }

    private void serviceDetailById(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        ServiceDao dao = new ServiceDao();
        Service service = dao.findServiceById(Integer.parseInt(req.getParameter("id")));
        AccountDao accountDao = new AccountDao();
        Account account = accountDao.findAccountById(service.getAccountId());
        service.setAccount(account);
        CostDao costDao = new CostDao();
        Cost cost = costDao.findCostById(service.getCostId());
        service.setCost(cost);
        req.setAttribute("service", service);
        req.getRequestDispatcher("WEB-INF/service/service_detail.jsp").forward(req, res);
    }

    private void findServices(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Integer currentPage = 1;
        if (req.getParameter("currentPage") != null) {
            currentPage = Integer.parseInt(req.getParameter("currentPage"));
        }
        ServiceDao dao = new ServiceDao();
        List<Service> services = dao.findServices();
        AccountDao accountDao = new AccountDao();
        CostDao costDao = new CostDao();
        for (int i = 0; i < services.size(); i++) {
            Account account = accountDao.findAccountById(services.get(i).getAccountId());
            services.get(i).setAccount(account);
            Cost cost = costDao.findCostById(services.get(i).getCostId());
            services.get(i).setCost(cost);
        }
        int serviceCount = services.size();
        int pageCount = serviceCount / singlePageLimit + (serviceCount % singlePageLimit > 0 ? 1 : 0);
        if (pageCount == 0) {
            pageCount = 1;
        }
        int lastPageCostCount = serviceCount - (pageCount - 1) * singlePageLimit;
        if (currentPage > pageCount) {
            currentPage = pageCount;
        }
        services = services.subList((currentPage - 1) * singlePageLimit, (currentPage - 1) * singlePageLimit + (currentPage == pageCount ?
                lastPageCostCount : singlePageLimit));
        req.setAttribute("currentPage", currentPage);
        req.setAttribute("pageCount", pageCount);
        req.setAttribute("services", services);
        req.setAttribute("path", "findServices.do");
        req.getRequestDispatcher("WEB-INF/service/service_list.jsp").forward(req, res);
    }

    private void toAddService(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        CostDao costDao = new CostDao();
        List<Cost> costs = costDao.findCosts();
        req.setAttribute("costs", costs);
        AccountDao accountDao = new AccountDao();
        List<Account> accounts = accountDao.findAccounts();
        req.setAttribute("accounts", accounts);
        req.getRequestDispatcher("WEB-INF/service/service_add.jsp").forward(req, res);
    }

    private void addService(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");
        if (!password.equals(confirmPassword)) {
            req.setAttribute("error", "两次密码输入不一致");
            req.getRequestDispatcher("toAddService.do").forward(req, res);
            return;
        }
        String idcard = req.getParameter("idcard");
        String costName = req.getParameter("costName");
        String host = req.getParameter("host");
        String osUsername = req.getParameter("osUsername");
        ServiceDao serviceDao = new ServiceDao();
        Service s = serviceDao.findServiceByOsUserame(osUsername);
        if (s != null) {
            String errorMsg = "保存失败，该登录名已被使用！";
            errorMsg = new String(errorMsg.getBytes("UTF-8"), "iso-8859-1");
            res.sendRedirect("toAddService.do?error='" + errorMsg + "'");
            return;
        }
        AccountDao accountDao = new AccountDao();
        Account account = accountDao.findAccountByIdCard(idcard);
        CostDao costDao = new CostDao();
        Cost cost = costDao.findCostByName(costName);
        Service service = new Service();
        service.setAccountId(account.getAccountId());
        service.setCostId(cost.getCostId());
        service.setUnixHost(host);
        service.setOsUsername(osUsername);
        service.setLoginPasswd(password);
        serviceDao.addService(service);
        res.sendRedirect("findServices.do");
    }

    private void toModifyService(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (req.getParameter("serviceId") == null) {
            throw new RuntimeException("业务账号id不能为空");
        }
        int serviceId = Integer.parseInt(req.getParameter("serviceId"));
        ServiceDao serviceDao = new ServiceDao();
        Service service = serviceDao.findServiceById(serviceId);
        CostDao costDao = new CostDao();
        List<Cost> costs = costDao.findCosts();
        Cost cost = costDao.findCostById(service.getCostId());
        req.setAttribute("service", service);
        req.setAttribute("costs", costs);
        req.setAttribute("cost", cost);
        req.getRequestDispatcher("WEB-INF/service/service_modi.jsp").forward(req, res);
    }

    private void modifyService(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (req.getParameter("serviceId") == null) {
            throw new RuntimeException("业务账号id不能为空");
        }
        Integer serviceId = Integer.parseInt(req.getParameter("serviceId"));
        Service service = new Service();
        service.setServiceId(serviceId);
        String costName = req.getParameter("costName");
        CostDao costDao = new CostDao();
        Cost cost = costDao.findCostByName(costName);
        service.setCost(cost);
        ServiceDao serviceDao = new ServiceDao();
        serviceDao.updateService(service);
        res.sendRedirect("findServices.do");
    }

    private void deleteService(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (req.getParameter("serviceId") == null) {
            throw new RuntimeException("业务账号id不能为空");
        }
        Integer serviceId = Integer.parseInt(req.getParameter("serviceId"));
        ServiceDao serviceDao = new ServiceDao();
        serviceDao.deleteService(serviceId);
        res.sendRedirect("findServices.do?deleteSuccess=1");
    }

    private void searchServices(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Integer currentPage = 1;
        if (req.getParameter("currentPage") != null) {
            currentPage = Integer.parseInt(req.getParameter("currentPage"));
        }
        String username = req.getParameter("username");
        String host = req.getParameter("host");
        String idcard = req.getParameter("idcard");
        String status = req.getParameter("status");
        req.setAttribute("status", status);
        req.setAttribute("idcard", idcard);
        req.setAttribute("username", username);
        req.setAttribute("host", host);
        ServiceDao dao = new ServiceDao();
        List<Service> services = dao.searchServices(status, idcard, username, host);
        AccountDao accountDao = new AccountDao();
        CostDao costDao = new CostDao();
        for (int i = 0; i < services.size(); i++) {
            Account account = accountDao.findAccountById(services.get(i).getAccountId());
            services.get(i).setAccount(account);
            Cost cost = costDao.findCostById(services.get(i).getCostId());
            services.get(i).setCost(cost);
        }
        int servicesCount = services.size();
        int pageCount = servicesCount / singlePageLimit + (servicesCount % singlePageLimit > 0 ? 1 : 0);
        if (pageCount == 0) {
            pageCount = 1;
        }
        int lastPageCostCount = servicesCount - (pageCount - 1) * singlePageLimit;
        if (currentPage > pageCount) {
            currentPage = pageCount;
        }
        services = services.subList((currentPage - 1) * singlePageLimit, (currentPage - 1) * singlePageLimit + (currentPage == pageCount ?
                lastPageCostCount : singlePageLimit));
        req.setAttribute("currentPage", currentPage);
        req.setAttribute("pageCount", pageCount);
        req.setAttribute("services", services);
        req.setAttribute("path", "searchServices.do");
        req.getRequestDispatcher("WEB-INF/service/service_list.jsp").forward(req, res);
    }

    private void setServiceState(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (req.getParameter("serviceId") == null) {
            throw new RuntimeException("业务账号id不能为空");
        }
        Integer serviceId = Integer.parseInt(req.getParameter("serviceId"));
        String status = req.getParameter("status");
        ServiceDao serviceDao = new ServiceDao();
        Service service = serviceDao.findServiceById(serviceId);
        AccountDao accountDao = new AccountDao();
        Account account = accountDao.findAccountById(service.getAccountId());
        if (!account.getStatus().equals("1")) {
            String errorMsg = "操作失败，所属账务账号未开通！";
            errorMsg = new String(errorMsg.getBytes("UTF-8"), "iso-8859-1");
            res.sendRedirect("findServices.do?error='" + errorMsg + "'");
            return;
        }
        serviceDao.setServiceState(serviceId, status);
        String successMsg = "开通业务账号成功！";
        if (status.equals("2")) {
            successMsg = "暂停业务账号成功！";
        }
        successMsg = new String(successMsg.getBytes("UTF-8"), "iso-8859-1");
        res.sendRedirect("findServices.do?success='" + successMsg + "'");
    }

//    private void startCalCost() {
//        ScheduledExecutorService service = Executors
//                .newSingleThreadScheduledExecutor();
//        // 第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间
//        service.scheduleAtFixedRate(new CalCostJob(), 0, 30, TimeUnit.DAYS);
//    }
//
//    class CalCostJob implements Runnable {
//        BillDao billDao;
//        CalCostJob(){
//            billDao=new BillDao();
//        }
//        @Override
//        public void run() {
//            billDao.addBill();
//        }
//    }
}
