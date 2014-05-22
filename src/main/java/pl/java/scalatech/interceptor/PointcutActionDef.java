/**
 * Copyright (C) 2012-2013 Risco-Software - All rights reserved.
 */

package pl.java.scalatech.interceptor;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * @author Sławomir Borowiec 
 * Module name : webTools_1113
 * Creating time :  27 sty 2014 14:54:47
 
 */
@Aspect
public class PointcutActionDef {
    @Pointcut("execution(* org.springframework.data.jpa.repository.JpaRepository+.find*(..))")
    public void cachefind() {

    }

    @Pointcut("execution(* org.springframework.data.repository.CrudRepository+.find(..))")
    public void cacheFindCrud() {

    }

    @Pointcut("execution(* org.springframework.data.repository.CrudRepository+.save(..)) && args(entity)"
            + " || execution(* org.springframework.data.repository.CrudRepository+.delete(..)) && args(entity)")
    public void cacheEvictCrud(PKEntity entity) {

    }

    @Pointcut("execution(* org.springframework.data.jpa.repository.JpaRepository+.saveAndFlush*(..)) && args(entity)")
    public void cacheEvict(PKEntity entity) {

    }
}
