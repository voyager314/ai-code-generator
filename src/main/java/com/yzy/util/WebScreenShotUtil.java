package com.yzy.util;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.yzy.common.AppConstant;
import com.yzy.exception.BusinessException;
import com.yzy.exception.ErrorCode;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.DestroyMode;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;

@Slf4j
@Component
public class WebScreenShotUtil {
    //使用commons-pool2进行WebDriver线程安全改造
    private static GenericObjectPool<WebDriver> webDriverPool;

    public WebScreenShotUtil() {
        GenericObjectPoolConfig<WebDriver> config = new GenericObjectPoolConfig<>();
        // 最多同时存在 5 个 Chrome 实例，每个大约200MB
        config.setMaxTotal(5);
        // 空闲时最多保留 2 个，其余销毁
        config.setMaxIdle(2);
        // 空闲时最少保留 0 个
        config.setMinIdle(0);
        // 借用等待超时
        config.setMaxWait(Duration.ofSeconds(60));
        // 借用前执行 validateObject 检查实例是否存活
        config.setTestOnBorrow(true);
        webDriverPool = new GenericObjectPool<>(new WebDriverFactory(), config);
    }

    @PreDestroy
    public void destroy() {
        if (webDriverPool != null) {
            webDriverPool.close();
        }
    }

    private static class WebDriverFactory implements PooledObjectFactory<WebDriver> {

        @Override
        public PooledObject<WebDriver> makeObject() throws Exception {
            return new DefaultPooledObject<>(initChromeDriver());
        }

        @Override
        public void destroyObject(PooledObject<WebDriver> p) {
            p.getObject().quit();
        }

        @Override
        public void destroyObject(PooledObject<WebDriver> p, DestroyMode destroyMode) {
            destroyObject(p);
        }

        @Override
        public boolean validateObject(PooledObject<WebDriver> p) {
            try {
                // 执行简单 JS 探活，抛异常说明 driver 已不可用
                ((JavascriptExecutor) p.getObject()).executeScript("return 1");
                return true;
            } catch (Exception e) {
                log.warn("WebDriver 健康检查失败，将从池中移除并销毁");
                return false;
            }
        }

        @Override
        public void activateObject(PooledObject<WebDriver> p) {
        }

        @Override
        public void passivateObject(PooledObject<WebDriver> p) {
            // 归还时导航到空白页，清除上次使用的页面状态，避免污染下次使用
            try {
                p.getObject().get("about:blank");
            } catch (Exception e) {
                log.warn("WebDriver 归还时重置页面失败", e);
            }
        }
    }

    /**
     * 初始化 Chrome 浏览器驱动
     */
    private static WebDriver initChromeDriver() {
        try {
            //WebDriverManager设置国内镜像
            System.setProperty("wdm.chromeDriverMirrorUrl", "https://registry.npmmirror.com/binary.html?path=chromedriver");
            // 自动管理 ChromeDriver
            WebDriverManager.chromedriver().useMirror().setup();
            // 配置 Chrome 选项
            ChromeOptions options = new ChromeOptions();
            // 无头模式
            options.addArguments("--headless");
            // 禁用GPU（在某些环境下避免问题）
            options.addArguments("--disable-gpu");
            // 禁用沙盒模式（Docker环境需要）
            options.addArguments("--no-sandbox");
            // 禁用开发者shm使用
            options.addArguments("--disable-dev-shm-usage");
            // 设置窗口大小
            options.addArguments(String.format("--window-size=%d,%d", 1600, 900));
            // 禁用扩展
            options.addArguments("--disable-extensions");
            // 设置用户代理
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            // 创建驱动
            WebDriver driver = new ChromeDriver(options);
            // 设置页面加载超时
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            // 设置隐式等待
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            return driver;
        } catch (Exception e) {
            log.error("初始化 Chrome 浏览器失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化 Chrome 浏览器失败");
        }
    }

    /**
     * 获取页面截图
     * @param webUrl 页面url
     * @return 压缩图片的路径
     */
    public static String getScreenShot(String webUrl) {
        if (StrUtil.isBlank(webUrl)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "页面地址为空！");
        }
        WebDriver webDriver = null;
        try {
            webDriver = webDriverPool.borrowObject();
            //图片临时路径
            String imgPath = AppConstant.IMG_DIR + File.separator + RandomUtil.randomString(8) + ".png";
            webDriver.get(webUrl);
            //等待页面加载
            WebDriverWait webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(10L));
            webDriverWait.until(driver -> ((JavascriptExecutor) driver)
                    .executeScript("return document.readyState").equals("complete"));
            //获取页面截图
            byte[] screenshot = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
            //保存图片
            FileUtil.writeBytes(screenshot, imgPath);
            String compressImgPath = AppConstant.IMG_DIR + File.separator + RandomUtil.randomString(8) + "_compress.png";
            File file = new File(imgPath);
            //压缩图片，这里为30%质量
            ImgUtil.compress(file, new File(compressImgPath), 0.3f);
            log.info("图片保存成功{}", compressImgPath);
            FileUtil.del(file);
            return compressImgPath;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取页面截图失败");
        } finally {
            // 无论成功或异常，都必须归还 driver，否则池会耗尽
            if (webDriver != null) {
                webDriverPool.returnObject(webDriver);
            }
        }
    }
}
