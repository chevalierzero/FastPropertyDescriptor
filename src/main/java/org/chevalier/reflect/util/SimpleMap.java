package org.chevalier.reflect.util;

/**
 * @author Chevalier (chevalier_zero@hotmail.com)
 */
public class SimpleMap<K, V>{
	
	// Integer缓存区中的最大值
	private static int capacityMax;
	private final Entry<K, V>[] table;
	
	static{
		
		int max = 127;
		// 获取配置信息中的Integer最大缓存数
		String integerCache = sun.misc.VM.getSavedProperty("java.lang.Integer.IntegerCache.high");
		
        if (integerCache != null) {

        	max = Math.min(Math.max(Integer.parseInt(integerCache), max), Integer.MAX_VALUE - 129);
        }
		// 如果配置信息中没有的话，则使用默认的最大缓存数 + 1
		capacityMax = max + 1;
	}
	
	public SimpleMap(){
		
		this(128);
	}
	
	@SuppressWarnings("unchecked")
	public SimpleMap(int capacity){
		
		if(capacity <= 0){
			
			throw new IllegalArgumentException("Illegal initial capacity: " + capacity);
		}
		
		table = new Entry[Math.min(capacity, capacityMax)];
	}
	
	public V get(K key){
		
		final int index = index(hash(key));
		
		Entry<K, V> entry = getEntry(key, index);
		
		return (entry != null) ? entry.value : null;
	}
	
	protected Entry<K, V> getEntry(K key, int index){
		
		for(Entry<K, V> entry = table[index]; entry != null; entry = entry.next){
			
			if(key.equals(entry.key)){
				
				return entry;
			}
		}
		
		return null;
	}
	
	public V put(K key, V value){
		
		V oldValue = null;
		final int hash = hash(key);
		final int index = index(hash);
		
		synchronized (Integer.valueOf(index)) {
			
			Entry<K, V> entry = getEntry(key, index);
			
			if(entry == null){

				table[index] = new Entry<K, V>(hash, key, value, table[index]);

			}else{
				
				oldValue = entry.value;
				entry.value = value;
			}
		}
		
		return oldValue;
	}
	
	private int hash(K key){
		
		return key.hashCode();
	}
	
	private int index(int hash){
		
		return hash & (table.length - 1);
	}

	public final static class Entry<K, V>{
		
		final int hashcode;
		final K key;
		volatile V value;
		final Entry<K, V> next;
		
		public Entry(int hashcode, K key, V value, Entry<K, V> next) {
			this.hashcode = hashcode;
			this.key = key;
			this.value = value;
			this.next = next;
		}
	}
}
