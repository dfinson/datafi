package org.sindaryn.datafi.service;

import lombok.val;
import org.sindaryn.datafi.persistence.GenericDao;
import org.sindaryn.datafi.reflection.CachedEntityType;
import org.sindaryn.datafi.reflection.ReflectionCache;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.StreamSupport;

import static com.google.common.collect.Maps.immutableEntry;
import static org.sindaryn.datafi.StaticUtils.*;

@Service
@SuppressWarnings("unchecked")
public abstract class BaseDataManager<T> {
    private Class<T> clazz;
    @Autowired
    protected ReflectionCache reflectionCache;
    /**
     * compile a list of all the the jpa repositories which were
     * autogenerated at compile time, and map each data access object
     * - or 'dao' to the name of its respective jpa repository.
     */
    private Map<String, GenericDao> daoMap;
    @Autowired//autowiring daos via proxy because cannot autowire directly in abstract class
    private DaoCollector daoCollector;
    @Autowired
    private EntityTypeRuntimeResolver<T> typeRuntimeResolver;

    public void setType(Class<T> type){
        this.clazz = type;
    }

    @PostConstruct
    private void init(){
        setType(typeRuntimeResolver.getType());
        daoMap = new HashMap<>();
        List<? extends GenericDao> daos = daoCollector.getDaos();
        daos.forEach(dao -> {
            String entityName = extractEntityName(dao);
            if(entityName != null)
                daoMap.put(entityName, dao);
        });
    }

    //spring framework instantiates proxies for each autowired instance.
    //if we want the actual name of the actual bean, we need to
    //'deproxy' the instance.
    private String extractEntityName(GenericDao dao) {
        val interfaces = ((Advised)dao).getProxiedInterfaces();
        String daoName = "";
        for(Class<?> interface_ : interfaces){
            if(interface_.getSimpleName().contains("Dao")){
                daoName = interface_.getSimpleName();
                break;
            }
        }
        int endIndex = daoName.indexOf("Dao");
        return endIndex != -1 ? daoName.substring(0, endIndex) : null;
    }

    public List<T> findAll(){return findAll(clazz);}

    public List<T> findAll(Class<T> clazz) {
        return daoMap.get(clazz.getSimpleName()).findAll();
    }

    public List<T> findAll(Sort sort) {
        return daoMap.get(clazz.getSimpleName()).findAll(sort);
    }

    public List<T> findAll(Class<T> clazz, Sort sort) {
        return daoMap.get(clazz.getSimpleName()).findAll(sort);
    }

    public Page<T> findAll(Pageable pageable) {return daoMap.get(clazz.getSimpleName()).findAll(pageable);}

    public Page<T> findAll(Class<T> clazz, Pageable pageable) {return daoMap.get(clazz.getSimpleName()).findAll(pageable);}

    public List<T> findAllById(Iterable<?> iterable) {return daoMap.get(clazz.getSimpleName()).findAllById(iterable);}

    public List<T> findAllById(Class<T> clazz, Iterable<?> iterable) {return daoMap.get(clazz.getSimpleName()).findAllById(iterable);}

    public long count() {return daoMap.get(clazz.getSimpleName()).count();}

    public long count(Class<T> clazz) {return daoMap.get(clazz.getSimpleName()).count();}

    public void deleteById(Object id) {
        daoMap.get(clazz.getSimpleName()).deleteById(id);
    }

    public void deleteById(Class<T> clazz, Object id) {
        daoMap.get(clazz.getSimpleName()).deleteById(id);
    }

    public void delete(T t) {
        daoMap.get(t.getClass().getSimpleName()).delete(t);
    }

    public void deleteAll(Iterable<? extends T> iterable) {
        long size = StreamSupport.stream(iterable.spliterator(), false).count();
        if(size <= 0) return;
        String clazzName = iterable.iterator().next().getClass().getSimpleName();
        daoMap.get(clazzName).deleteAll(iterable);
    }

    public void deleteAll() {
        daoMap.get(clazz.getSimpleName()).deleteAll();
    }

    public void deleteAll(Class<T> clazz) {
        daoMap.get(clazz.getSimpleName()).deleteAll();
    }

    public <S extends T> S save(S s) {
        return (S) daoMap.get(s.getClass().getSimpleName()).save(s);
    }

    public <S extends T> List<S> saveAll(Iterable<S> iterable) {
        long size = StreamSupport.stream(iterable.spliterator(), false).count();
        if(size <= 0) return new ArrayList<>();
        String clazzName = iterable.iterator().next().getClass().getSimpleName();
        return daoMap.get(clazzName).saveAll(iterable);
    }

    public Optional<T> findById(Object id) {
        return daoMap.get(clazz.getSimpleName()).findById(id);
    }

    public Optional<T> findById(Class<T> clazz, Object id) {
        return daoMap.get(clazz.getSimpleName()).findById(id);
    }

    public boolean existsById(Object id) {
        return daoMap.get(clazz.getSimpleName()).existsById(id);
    }

    public boolean existsById(Class<T> clazz, Object id) {
        return daoMap.get(clazz.getSimpleName()).existsById(id);
    }

    public void flush() {
        daoMap.get(clazz.getSimpleName()).flush();
    }

    public void flush(Class<T> clazz) {
        daoMap.get(clazz.getSimpleName()).flush();
    }

    public <S extends T> S saveAndFlush(S s) {
        String clazzName = s.getClass().getSimpleName();
        return (S) daoMap.get(clazzName).saveAndFlush(s);
    }

    public void deleteInBatch(Iterable<T> iterable) {
        long size = StreamSupport.stream(iterable.spliterator(), false).count();
        if(size <= 0) return;
        String clazzName = iterable.iterator().next().getClass().getSimpleName();
        daoMap.get(clazzName).deleteInBatch(iterable);
    }

    public void deleteAllInBatch() {
        daoMap.get(clazz.getSimpleName()).deleteAllInBatch();
    }

    public void deleteAllInBatch(Class<?> clazz) {
        daoMap.get(clazz.getSimpleName()).deleteAllInBatch();
    }

    public T getOne(Object id) {
        return (T) daoMap.get(clazz.getSimpleName()).getOne(id);
    }

    public T getOne(Class<T> clazz, Object id) {
        return (T) daoMap.get(clazz.getSimpleName()).getOne(id);
    }

    public <S extends T> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    public <S extends T> List<S> findAll(Example<S> example) {
        String clazzName = example.getProbe().getClass().getSimpleName();
        return daoMap.get(clazzName).findAll(example);
    }

    public <S extends T> List<S> findAll(Example<S> example, Sort sort) {
        String clazzName = example.getProbe().getClass().getSimpleName();
        return daoMap.get(clazzName).findAll(example, sort);
    }

    public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
        String clazzName = example.getProbe().getClass().getSimpleName();
        return daoMap.get(clazzName).findAll(example, pageable);
    }

    public <S extends T> long count(Example<S> example) {
        String clazzName = example.getProbe().getClass().getSimpleName();
        return daoMap.get(clazzName).count(example);
    }

    public <S extends T> boolean exists(Example<S> example) {
        String clazzName = example.getProbe().getClass().getSimpleName();
        return daoMap.get(clazzName).exists(example);
    }

    public List<T> getBy(String attributeName, Object attributeValue){
        return getBy(clazz, attributeName, attributeValue);
    }

    public List<T> getBy(Class<T> clazz, String attributeName, Object attributeValue){
        try{
            GenericDao dao = daoMap.get(clazz.getSimpleName());
            Class<?>[] params = new Class<?>[]{attributeValue.getClass()};
            String resolverName = "findBy" + toPascalCase(attributeName);
            Method methodToInvoke = getMethodToInvoke(resolverName, params, dao);
            return (List<T>) methodToInvoke.invoke(dao, new Object[]{attributeValue});
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public Optional<T> getByUnique(Class<T> clazz, String attributeName, Object attributeValue){
        try{
            GenericDao dao = daoMap.get(clazz.getSimpleName());
            Class<?>[] params = new Class<?>[]{attributeValue.getClass()};
            String resolverName = "findBy" + toPascalCase(attributeName);
            Method methodToInvoke = getMethodToInvoke(resolverName, params, dao);
            return (Optional<T>) methodToInvoke.invoke(dao, new Object[]{attributeValue});
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public List<T> getAllBy(String attributeName, Object[] attributeValues){
        return getAllBy(clazz, attributeName, attributeValues);
    }

    public List<T> getAllBy(Class<T> clazz, String attributeName, Object[] attributeValues){
        try{
            GenericDao dao = daoMap.get(clazz.getSimpleName());
            Class<?>[] params = new Class<?>[]{List.class};
            String resolverName = "findAllBy" + toPascalCase(attributeName) + "In";
            Method methodToInvoke = getMethodToInvoke(resolverName, params, dao);
            return (List<T>) methodToInvoke.invoke(dao, Arrays.asList(attributeValues));
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public Optional<T> findOne(Specification<T> specification) {
        return daoMap.get(clazz.getSimpleName()).findOne(specification);
    }

    public Optional<T> findOne(Class<T> clazz, Specification<T> specification) {
        return daoMap.get(clazz.getSimpleName()).findOne(specification);
    }

    public List<T> findAll(Specification<T> specification) {
        return daoMap.get(clazz.getSimpleName()).findAll(specification);
    }

    public List<T> findAll(Class<T> clazz, Specification<T> specification) {
        return daoMap.get(clazz.getSimpleName()).findAll(specification);
    }

    public Page<T> findAll(Specification<T> specification, Pageable pageable) {
        return daoMap.get(clazz.getSimpleName()).findAll(specification, pageable);
    }

    public Page<T> findAll(Class<T> clazz, Specification<T> specification, Pageable pageable) {
        return daoMap.get(clazz.getSimpleName()).findAll(specification, pageable);
    }

    public List<T> findAll(Specification<T> specification, Sort sort) {
        return daoMap.get(clazz.getSimpleName()).findAll(specification, sort);
    }

    public List<T> findAll(Class<T> clazz, Specification<T> specification, Sort sort) {
        return daoMap.get(clazz.getSimpleName()).findAll(specification, sort);
    }

    public long count(Specification<T> specification) {
        return daoMap.get(clazz.getSimpleName()).count(specification);
    }

    public long count(Class<T> clazz, Specification<T> specification) {
        return daoMap.get(clazz.getSimpleName()).count(specification);
    }

    public List<T> selectByResolver(String resolverName, Object... args){
        return selectByResolver(clazz, resolverName, args);
    }

    public List<T> selectByResolver(Class<?> clazz, String resolverName, Object... args){
        try{
            GenericDao dao = daoMap.get(clazz.getSimpleName());
            Class<?>[] params = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) params[i] = args[i].getClass();
            Method methodToInvoke = getMethodToInvoke(resolverName, params, dao);
            return (List<T>) methodToInvoke.invoke(dao, args);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public T cascadedUpdate(T toUpdate, T source){
        return (T) cascadedUpdateImpl(toUpdate, source);
    }

    public<HasTs> List<T> addNewToCollectionIn(HasTs toAddTo, String fieldName, List<T> toAdd){

        final String toAddName = toAdd.get(0).getClass().getSimpleName();
        GenericDao toAddDao = daoMap.get(toAddName);
        final String toAddToName = toAddTo.getClass().getSimpleName();
        GenericDao toAddToDao = daoMap.get(toAddToName);

        toAddTo = (HasTs) toAddToDao.findById(reflectionCache.getEntitiesCache().get(toAddToName).invokeGetter(toAddTo, "id")).orElse(null);
        if(toAddTo == null) throw new IllegalArgumentException("Could not find an entity with the given id");
        Method existingCollectionGetter = getMethodToInvoke("get" + toPascalCase(fieldName) ,toAddTo);
        Collection<T> existingCollection = (Collection<T>) invoke(existingCollectionGetter, toAddTo);
        existingCollection.addAll(toAdd);
        Method existingCollectionSetter = getMethodToInvoke("set" + toPascalCase(fieldName) ,toAddTo);
        invoke(existingCollectionSetter, toAddTo, existingCollection);

        toAddToDao.save(toAddTo);
        toAddDao.saveAll(toAdd);
        return toAdd;
    }

    public<HasTs> List<T> attachExistingToCollectionIn(HasTs toAddTo, String fieldName, List<T> toAttach){

        final String toAddName = toAttach.get(0).getClass().getSimpleName();
        GenericDao toAddDao = daoMap.get(toAddName);
        final String toAddToName = toAddTo.getClass().getSimpleName();
        GenericDao toAddToDao = daoMap.get(toAddToName);

        toAttach = toAddDao.findAllById(idList(toAttach));
        toAddTo = (HasTs) toAddToDao.findById(reflectionCache.getEntitiesCache().get(toAddToName).invokeGetter(toAddTo, "id")).orElse(null);
        if(toAddTo == null) throw new IllegalArgumentException("Could not find an entity with the given id");
        Method existingCollectionGetter = getMethodToInvoke("get" + toPascalCase(fieldName) ,toAddTo);
        Collection<T> existingCollection = (Collection<T>) invoke(existingCollectionGetter, toAddTo);
        existingCollection.addAll(toAttach);
        Method existingCollectionSetter = getMethodToInvoke("set" + toPascalCase(fieldName) ,toAddTo);
        invoke(existingCollectionSetter, toAddTo, existingCollection);
        toAddToDao.save(toAddTo);
        return toAttach;
    }

    public List<T> cascadeUpdateCollection(Iterable<T> toUpdate, Iterable<T> updated){
        GenericDao dao = daoMap.get(toUpdate.iterator().next().getClass().getSimpleName());
        Iterator<T> updatedEntitiesIterator = updated.iterator();
        Iterator<T> entitiesToUpdateIterator = toUpdate.iterator();
        T entityToUpdate, updatedEntity;

        while(updatedEntitiesIterator.hasNext() && entitiesToUpdateIterator.hasNext()){
            updatedEntity = updatedEntitiesIterator.next();
            entityToUpdate = entitiesToUpdateIterator.next();
            cascadedUpdateImpl(entityToUpdate, updatedEntity);
        }
        return dao.saveAll(toUpdate);
    }

    private Object cascadedUpdateImpl(Object toUpdate, Object source){
        Class<?> currentClazz = toUpdate.getClass();
        Collection<Field> fieldsToUpdate = reflectionCache.getEntitiesCache().get(currentClazz.getSimpleName()).getCascadeUpdatableFields();
        for(Field currentField : fieldsToUpdate){
            try {
                currentField.setAccessible(true);
                Object sourceFieldValue = currentField.get(source);
                Object targetFieldValue = currentField.get(toUpdate);
                //if field value is null, there's nothing to update to
                if(sourceFieldValue == null) continue;
                //if field is an embedded entity, we need to recursively update all of its fields
                if(isForeignKey(currentField, toUpdate))
                    cascadedUpdateImpl(targetFieldValue, sourceFieldValue);
                    //if field is a collection, that's outside of this use case,
                else
                    //else, (...finally) update field value
                    currentField.set(toUpdate, sourceFieldValue);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        return daoMap.get(currentClazz.getSimpleName()).save(toUpdate);
    }

    private boolean isForeignKey(Field currentField, Object owner) {
        try{
            currentField.setAccessible(true);
            boolean isForeignKey = currentField.isAnnotationPresent(OneToOne.class) ||
                    currentField.isAnnotationPresent(ManyToOne.class);
            if(isForeignKey) {
                if(currentField.get(owner) == null)
                    currentField.set(owner, defaultInstanceOf(currentField.getType()));
                return true;
            }
            return false;
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private Object defaultInstanceOf(Class<?> type) {
        return reflectionCache.getEntitiesCache().get(type.getSimpleName()).getDefaultInstance();
    }

    private Method getMethodToInvoke(String resolverName, Object instance) {
        return getMethodToInvoke(resolverName, new Class<?>[]{}, instance);
    }

    private Method getMethodToInvoke(String resolverName, Class<?>[] params, Object instance){
        Method methodToInvoke = reflectionCache.getResolversCache().get(immutableEntry(resolverName, params));
        if(methodToInvoke == null){
            try {
                if(params.length > 0)
                    methodToInvoke = instance.getClass().getMethod(resolverName, params);
                else
                    methodToInvoke = instance.getClass().getMethod(resolverName);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            reflectionCache.getResolversCache().put(immutableEntry(resolverName, params), methodToInvoke);
        }
        return methodToInvoke;
    }

    private Object invoke(Method method, Object instance, Object... args){
        try{
            if(args.length > 0) return method.invoke(instance, args);
            else return method.invoke(instance);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public List<Object> idList(Iterable<T> collection) {
        CachedEntityType entityType = reflectionCache.getEntitiesCache().get(collection.iterator().next().getClass().getSimpleName());
        List<Object> ids = new ArrayList<>();
        collection.forEach(item -> ids.add(entityType.invokeGetter(item, "id")));
        return ids;
    }

    public org.sindaryn.datafi.reflection.ReflectionCache getReflectionCache() {
        return this.reflectionCache;
    }

    //implicit / default pagination
    public List<T> fuzzySearchBy(String searchTerm){
        return fuzzySearchBy(clazz, searchTerm);
    }
    public List<T> fuzzySearchBy(Class<T> clazz, String searchTerm){
        return fuzzySearchBy(clazz, searchTerm, 0, 50);
    }

    //explicit pagination
    public List<T> fuzzySearchBy(String searchTerm, int offset, int limit){
        return fuzzySearchBy(clazz, searchTerm, offset, limit);
    }
    public List<T> fuzzySearchBy(Class<T> clazz, String searchTerm, int offset, int limit){
        return fuzzySearchBy(clazz, searchTerm, offset, limit, null, null);
    }

    //explicit pagination with sort

    public List<T> fuzzySearchBy(String searchTerm, int offset, int limit, String sortBy, Sort.Direction sortDirection){
        return fuzzySearchBy(clazz, searchTerm, offset, limit, sortBy, sortDirection);
    }

    public List<T> fuzzySearchBy(
            Class<T> clazz, String searchTerm, int offset, int limit, String sortBy, Sort.Direction sortDirection){
        try{
            if(searchTerm.equals(""))
                throw new IllegalArgumentException(
                        "Illegal attempt to search for " + toPlural(clazz.getSimpleName()) + " with blank string"
                );
            validateSortByIfNonNull(clazz, sortBy, reflectionCache);
            GenericDao dao = daoMap.get(clazz.getSimpleName());
            Pageable paginator = generatePageRequest(offset, limit, sortBy, sortDirection);
            Method methodToInvoke =
                    getMethodToInvoke("fuzzySearch", new Class<?>[]{String.class, Pageable.class}, dao);
            Page<T> result = (Page<T>) methodToInvoke.invoke(dao, searchTerm, paginator);
            return result.getContent();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

}
