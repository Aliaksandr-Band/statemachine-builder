package alex.band.statemachine.context;

import java.util.HashMap;
import java.util.Map;


/**
 * Implementation of {@link StateMachineContext}.
 *
 * @author Aliaksandr Bandarchyk
 */
public class StateMachineContextImpl implements StateMachineContext {

	private Map<String, Object> values = new HashMap<>();

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

}
