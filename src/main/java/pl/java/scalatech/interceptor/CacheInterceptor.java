/**
 * Copyright (C) 2012-2013 Risco-Software - All rights reserved.
 */

package pl.java.scalatech.interceptor;

import java.lang.reflect.Field;

import lombok.extern.slf4j.Slf4j;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.interceptor.DefaultKeyGenerator;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Component;

import pl.java.risco.annotations.WebToolComponent;
import pl.java.risco.common_entity.PKEntity;


@Aspect
@Component
@Slf4j
public class CacheInterceptor {

    private EhCacheCacheManager ehCacheCacheManager;

    private KeyGenerator keyGenerator = new DefaultKeyGenerator();

    private CacheManager cm;

    @Autowired
    public CacheInterceptor(EhCacheCacheManager ehCacheCacheManager) {
        this.ehCacheCacheManager = ehCacheCacheManager;
        this.cm = ehCacheCacheManager.getCacheManager();
    }

    @Around("PointcutActionDef.cachefind()")
    public Object cachefind(ProceedingJoinPoint pjp) throws Throwable {
        return getPotencialObjectFromCache(pjp);
    }

    @Around("PointcutActionDef.cacheFindCrud()")
    public Object cacheCrudfind(ProceedingJoinPoint pjp) throws Throwable {
        return getPotencialObjectFromCache(pjp);
    }

    @Around("PointcutActionDef.cacheEvictCrud(entity)")
    public Object cacheEvictCrud(ProceedingJoinPoint pjp, PKEntity entity) throws Throwable {
        return saveOrDeleteAction(pjp, entity);
    }

    @Around("PointcutActionDef.cacheEvict(entity)")
    public Object cacheEvict(ProceedingJoinPoint pjp, PKEntity entity) throws Throwable {
        Object object = saveOrDeleteAction(pjp, entity);

        return object;
    }

    private Object getPotencialObjectFromCache(ProceedingJoinPoint pjp) throws Throwable {
        String cacheName = retrieveEntityTypeAsString(pjp);
        cm = ehCacheCacheManager.getCacheManager();
        Cache cache = cm.getCache(cacheName);
        if (cache == null) {
            log.debug("++                                                      cache is null - > undefined -->  where cacheName {}", cacheName);
        }
        Object key = keyGenerator.generate(pjp.getTarget(), null, pjp.getArgs());
        if (cache == null) {
            log.debug("-->                                   put into cacheName  {}   : key  {}", cacheName, key);
            return putIntoCacheEntity(pjp, cache, key);
        }
        Element element = cache.get(key);
        if (element != null) {
            Object fromCache = element.getObjectValue();
            log.debug("<--                                 retrieve from cacheName  {}   : key  {}", cacheName, key);
            return fromCache;
        }
        log.debug("-->                                   put into cacheName  {}   : key  {}", cacheName, key);
        return putIntoCacheEntity(pjp, cache, key);
    }

    private Object putIntoCacheEntity(ProceedingJoinPoint pjp, Cache cache, Object key) throws Throwable {
        Object returnVal = pjp.proceed();
        Element element = new Element(key, returnVal);
        if (cache != null) {
            cache.put(element);
        }
        return returnVal;
    }

    private PKEntity saveOrDeleteAction(ProceedingJoinPoint pjp, PKEntity entity) throws Throwable {
        cm = ehCacheCacheManager.getCacheManager();
        String cacheName = entity.getClass().getSimpleName().toLowerCase();
        Cache cache = cm.getCache(cacheName);
        if (!cm.cacheExists(cacheName)) {
            return (PKEntity) pjp.proceed();
        }
        cache.removeAll();
        cache.flush();
        log.debug("++                                         clear cache {}      ", cacheName);
        SimpleJpaRepository<?, ?> repo = (SimpleJpaRepository<?, ?>) ((Advised) pjp.getTarget()).getTargetSource().getTarget();
        repo.flush();
        return (PKEntity) pjp.proceed();
    }

    private String retrieveEntityTypeAsString(ProceedingJoinPoint pjp) throws Exception {
        SimpleJpaRepository<?, ?> repo = (SimpleJpaRepository<?, ?>) ((Advised) pjp.getTarget()).getTargetSource().getTarget();
        Field entityInformation = repo.getClass().getDeclaredField("entityInformation");
        entityInformation.setAccessible(true);
        JpaEntityInformation<?, ?> o = (JpaEntityInformation<?, ?>) entityInformation.get(repo);
        return  o.getJavaType().getSimpleName().toLowerCase();
    }

    public void clearCache(Class<?> clazz) {
        cm = ehCacheCacheManager.getCacheManager();
        String cacheName = clazz.getSimpleName().toLowerCase();
        Cache cache = cm.getCache(cacheName);
        if (cache == null) {
            return;
        }
        cache.removeAll();
        cache.flush();
    }
}
