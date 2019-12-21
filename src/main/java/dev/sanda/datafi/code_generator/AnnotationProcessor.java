package dev.sanda.datafi.code_generator;


import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeSpec;
import dev.sanda.datafi.StaticUtils;
import dev.sanda.datafi.code_generator.query.CustomSQLQueryFactory;
import lombok.val;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Takes care of generating all the source files needed for a jpa data access layer.
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class AnnotationProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        Set<? extends TypeElement> entities = getPersistableEntities(roundEnvironment);
        if(entities.isEmpty()) return false;
        val customSqlQueriesMap = new CustomSQLQueryFactory(processingEnv).constructCustomQueries(entities);
        val searchMethodsMap = new FreeTextSearchMethodsFactory(processingEnv).resolveFreeTextSearchMethods(entities);
        Map<TypeElement, List<VariableElement>> annotatedFieldsMap =
                new FindByFieldsResolver(processingEnv).annotatedFieldsMap(entities);
        //generate a custom jpa repository for each entity
        DaoFactory daoFactory = new DaoFactory(processingEnv);
        DataManagerFactory dataManagerFactory = new DataManagerFactory(processingEnv, StaticUtils.getBasePackage(roundEnvironment));
        entities.forEach(entity -> {
            daoFactory.generateDao(entity, annotatedFieldsMap, customSqlQueriesMap, searchMethodsMap);
            dataManagerFactory.addDataManager(entity);
            if(StaticUtils.isArchivable(entity, processingEnv))
                dataManagerFactory.addArchivableDataManager(entity);
        });
        dataManagerFactory.addBasePackageResolver();
        dataManagerFactory.writeToFile();
        /*
        create a configuration source file such that
        generated spring beans are included within
        the runtime target application context
        */
        setComponentScan(entities);
        //return false - these annotations are needed for the web-service layer as well
        return false;

    }

    /**
     * compile and return a list of all entities annotated
     * with @Entity or @Table, and as such
     * relevant to the code generator
     * @param roundEnvironment
     * @return
     */
    private Set<? extends TypeElement> getPersistableEntities(RoundEnvironment roundEnvironment) {
        return StaticUtils.getEntitiesSet(roundEnvironment);
    }

    private void setComponentScan(Set<? extends TypeElement> entities) {
        if(!entities.isEmpty()){
            String className = entities.iterator().next().getQualifiedName().toString();
            int lastdot = className.lastIndexOf('.');
            String basePackageName = className.substring(0, lastdot);
            String simpleClassName = "SandaClasspathConfiguration";
            TypeSpec.Builder builder = TypeSpec.classBuilder(simpleClassName)
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Configuration.class)
                    .addAnnotation(AnnotationSpec.builder(ComponentScan.class)
                            .addMember(
                                    "basePackages",
                                    "{$S}",
                                    "dev.sanda")
                            .build());
            StaticUtils.writeToJavaFile(simpleClassName, basePackageName, builder, processingEnv, "configuration source file");
        }
    }
}