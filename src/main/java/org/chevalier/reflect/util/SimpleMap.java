package org.chevalier.reflect.util;

/**
 * @author Chevalier (chevalier_zero@hotmail.com)
 */
public class SimpleMap<K, V>{
	
	private final Entry<K, V>[] table;
	
	public SimpleMap(){
		
		this(128);
	}
	
	@SuppressWarnings("unchecked")
	public SimpleMap(int capacity){
		
		table = new Entry[capacity];
	}
	
	public V get(K key){
		
		final int index = index(hash(key));
		
		Entry<K, V> entry = getEntry(key, index);
		
		return (entry != null) ? entry.value : null;
	}
	
	private Entry<K, V> getEntry(K key, int index){
		
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
	
	public int hash(K key){
		
		return key.hashCode();
	}
	
	public int index(int hash){
		
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
