package com.mtons.mblog.config;

import com.mtons.mblog.base.lang.Consts;
import com.mtons.mblog.base.utils.Printer;
import com.mtons.mblog.modules.entity.Options;
import com.mtons.mblog.modules.service.ChannelService;
import com.mtons.mblog.modules.service.OptionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 加载配置信息到系统
 *
 */
@Component
public class ContextStartup implements ApplicationRunner, Ordered, ServletContextAware {
    @Autowired
    private OptionsService optionsService;
    @Autowired
    private ChannelService channelService;
    @Autowired
    private SiteOptions siteOptions;

    private ServletContext servletContext;

    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {
        Timer timer = new Timer("startup");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Printer.info("initialization ...");

                resetSetting(true);
                resetSiteConfig();
                resetChannels();

                Printer.info("OK, completed");
            }
        }, 1 * Consts.TIME_MIN);
    }

    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * 重置站点配置
     */
    public void resetSetting(boolean exit) {
        List<Options> options = optionsService.findAll();

        if (null == options || options.isEmpty()) {
            try {
                Resource resource = new ClassPathResource("/config/db/db_mblog.sql");
                optionsService.initSettings(resource);
            } catch (Exception e) {
                Printer.error("------------------------------------------------------------");
                Printer.error("-  ERROR:The SQL file is not imported. (sql/db_mblog.sql)  -");
                Printer.error("-         Please import the SQL file and try again.        -");
                Printer.error("------------------------------------------------------------");
                Printer.error(e.getMessage(), e);
                if (exit) {
                    System.exit(1);
                }
            }
        } else {
            options.forEach(conf -> {
                siteOptions.getOptions().put(conf.getKey(), conf.getValue());
                servletContext.setAttribute(conf.getKey(), conf.getValue());
            });

            servletContext.setAttribute("options", siteOptions.getOptions());
        }
    }

    /**
     * 重置栏目缓存
     */
    public void resetChannels() {
        servletContext.setAttribute("channels", channelService.findAll(Consts.STATUS_NORMAL));
    }

    public void resetSiteConfig() {
        servletContext.setAttribute("site", siteOptions);
    }
}
