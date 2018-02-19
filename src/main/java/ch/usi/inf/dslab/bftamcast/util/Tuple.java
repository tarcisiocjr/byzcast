/**
 * 
 */
package ch.usi.inf.dslab.bftamcast.util;

/**
 * @author Christian Vuerich - christian.vuerich@usi.ch
 *	
 * temporary class in case need to store the destination proxy for async clients requests and the majority counter
 */
public class Tuple<A,B> {
	public A a;
	public B b;
	
	public Tuple(A a, B b) {
		this.a = a;
		this.b =b;
	}
}