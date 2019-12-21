package dev.sanda.datafi.code_generator;

import com.squareup.javapoet.*;
import dev.sanda.datafi.StaticUtils;
import dev.sanda.datafi.annotations.free_text_search.FreeTextSearchBy;
import dev.sanda.datafi.annotations.free_text_search.FreeTextSearchByFields;
import lombok.Data;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.*;

import static com.squareup.javapoet.ParameterizedTypeName.get;

@Data
public class FreeTextSearchMethodsFactory {

    @NonNull
    private ProcessingEnvironment processingEnv;

    protected Map<TypeElement, MethodSpec> resolveFreeTextSearchMethods(Set<? extends TypeElement> entities) {
        Map<TypeElement, MethodSpec> result = new HashMap<>();
        for (TypeElement entity : entities) {
            List<VariableElement> searchFields = getSearchFieldsOf(entity);
            if(!searchFields.isEmpty()){
                MethodSpec freeTextSearchMethod = generateFreeTextSearchMethod(entity, searchFields);
                result.put(entity, freeTextSearchMethod);
            }
        }
        return result;
    }

    public List<VariableElement> getSearchFieldsOf(TypeElement entity) {

        List<String> classLevelSearchByFieldNames = new ArrayList<>();
        FreeTextSearchByFields classLevelSearchByAnnotation = entity.getAnnotation(FreeTextSearchByFields.class);
        if(classLevelSearchByAnnotation != null)
            classLevelSearchByFieldNames.addAll(Arrays.asList(classLevelSearchByAnnotation.fields()));

        List<VariableElement> searchFields = new ArrayList<>();
        List<? extends Element> enclosedElements = entity.getEnclosedElements();
        for (Element enclosedElement : enclosedElements)
            if (isSearchByField(classLevelSearchByFieldNames, enclosedElement))
                searchFields.add((VariableElement) enclosedElement);

        return searchFields;
    }

    private MethodSpec generateFreeTextSearchMethod(TypeElement entity, List<VariableElement> searchFields) {
        String entityName = entity.getSimpleName().toString();
        String methodName = "freeTextSearch";
        ParameterSpec argument = ParameterSpec.builder(String.class, "searchTerm")
                .addAnnotation(AnnotationSpec.builder(Param.class)
                        .addMember("value", "$S", "searchTerm")
                        .build())
                .build();
        String freeTextSearchQuery = freeTextSearchQuery(entityName, searchFields);
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                .addParameter(argument)
                .addParameter(Pageable.class, "paginator")
                .addAnnotation(AnnotationSpec.builder(Query.class)
                        .addMember("value", "$S", freeTextSearchQuery)
                        .build())
                .returns(get(ClassName.get(Page.class), ClassName.get(entity)))
                .build();
    }

    private boolean isSearchByField(List<String> classLevelSearchByFieldNames, Element enclosedElement) {
        boolean isDeclaredAsSearchBy = enclosedElement.getKind().isField() &&
                (enclosedElement.getAnnotation(FreeTextSearchBy.class) != null ||
                        classLevelSearchByFieldNames.contains(enclosedElement.getSimpleName().toString()));
        if(isDeclaredAsSearchBy && !"java.lang.String".equals(enclosedElement.asType().toString())){
            StaticUtils.logCompilationError(processingEnv, enclosedElement,
                    "field " + enclosedElement.asType().toString() + " " +
                            enclosedElement.getSimpleName().toString() + " is marked as free text search parameter" +
                            " but is not of type java.lang.String");
            return false;
        }
        return isDeclaredAsSearchBy;
    }

    private String freeTextSearchQuery(String entityName, List<VariableElement> searchFields) {
        String placeHolder = StaticUtils.firstLowerCaseLetterOf(entityName);
        StringBuilder result = new StringBuilder("SELECT " + placeHolder + " FROM " + entityName + " " + placeHolder);
        boolean isFirst = true;
        for (VariableElement field : searchFields) {
            final String conditionPrefix = isFirst ? " WHERE" : " OR";
            isFirst = false;
            final String condition = " lower(" + placeHolder + "." + field.getSimpleName() + ") " +
                    "LIKE lower(concat('%', :searchTerm, '%'))";
            result.append(conditionPrefix);
            result.append(condition);
        }
        return result.toString();
    }
}