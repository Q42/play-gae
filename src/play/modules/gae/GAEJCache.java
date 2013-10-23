package play.modules.gae;

import java.util.Collections;
import com.google.appengine.api.memcache.MemcacheService;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheFactory;
import net.sf.jsr107cache.CacheManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import play.Logger;
import play.Play;
import play.cache.CacheImpl;
import play.exceptions.CacheException;

public class GAEJCache implements CacheImpl { 

    private Cache cache;

	public GAEJCache() throws CacheException {
		try {
            CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
            cache = cacheFactory.createCache(Collections.emptyMap());
        } catch (net.sf.jsr107cache.CacheException e) {
            Logger.error( "Cannot create cache ", e) ;
            throw new CacheException("Cannot create cache", e);

        }
	}
    
    private void put(String key, Object value, int expiration){
	  // JCache doesn't let specify the expiration at the item level.
	  // For more info, see https://developers.google.com/appengine/docs/java/memcache/usingjcache
	  cache.put(key, wrap(value)) ;
    }
    public void add(String key, Object value, int expiration) {
        put(key, value, expiration);
    }

    public boolean safeAdd(String key, Object value, int expiration) {
        put(key, value, expiration);
        return true;
    }

    public void set(String key, Object value, int expiration) {
       put(key, value, expiration);
    }

    public boolean safeSet(String key, Object value, int expiration) {
        put(key, value, expiration);
        return true;
    }

    public void replace(String key, Object value, int expiration) {
        put(key, value, expiration);
    }

    public boolean safeReplace(String key, Object value, int expiration) {
        put(key, value, expiration);
        return true;
    }

    public Object get(String key) {
        return unwrap(cache.get(key));
    }

    public Map<String, Object> get(String[] keys) {
        List<String> list = Arrays.asList(keys);
		Map<String,Object> result = new HashMap<String,Object>();
		try
		{
			Map<String, Object> map = cache.getAll(list);
        
			for(Object key : map.entrySet()) {
				result.put(key.toString(), unwrap(map.get(key)));
			}
		}
		catch (net.sf.jsr107cache.CacheException e) {
            Logger.error( "Error while tryng to get cache values", e) ;
            throw new CacheException("Error while tryng to get cache values", e);

        }
        return result ;
    }

    public long incr(String key, int by) {
		Logger.info( "CACHE : JCache does not support incr( " + key + ", " + by + ")") ;
		return 0 ; 
    }

    public long decr(String key, int by) {
 		Logger.info( "CACHE : JCache does not support decr( " + key + ", " + by + ")") ;
		return 0 ;
    }

    public void clear() {
        cache.clear() ;
    }

    public void delete(String key) {
		cache.remove(key) ;
    }

    public boolean safeDelete(String key) {
		cache.remove(key) ;
        return true;
    }

    public void stop() {
    }

    byte[] wrap(Object o) {
        if(o == null) {
            return null;
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bytes);
            oos.writeObject(o);
            return bytes.toByteArray();
        } catch (Exception e) {
			Logger.error( "Cannot cache a non-serializable value of type " + o.getClass().getName(), e) ;
            throw new CacheException("Cannot cache a non-serializable value of type " + o.getClass().getName(), e);
        }
    }

    Object unwrap(Object bytes) {
        if(bytes == null) {
            return null;
        }
        try {
            return new ObjectInputStream(new ByteArrayInputStream((byte[])bytes)) {

                @Override
                protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                    return Class.forName(desc.getName(), false, Play.classloader);
                }
            }.readObject();
        } catch (Exception e) {
            Logger.error(e, "Error while deserializing cached value");
            return null;
        }
    }
}
