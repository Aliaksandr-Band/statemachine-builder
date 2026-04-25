package alex.band.statemachine.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Implementation of {@link StateMachineContext}.
 *
 * @author Aliaksandr Bandarchyk
 */
public class StateMachineContextImpl implements StateMachineContext {

	private Map<String, Object> values = new ConcurrentHashMap<>();

	@Override
	public Object getValue(String key) {
		return values.get(key);
	}

	@Override
	public void setValue(String key, Object value) {
		values.put(key, value);
	}

	@Override
	public Object removeValue(String key) {
		return values.remove(key);
	}

	@Override
	public void clear() {
		values.clear();
	}

}
