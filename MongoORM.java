import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class MongoORM {

	protected Map<Object, ObjectId> pickled = new HashMap<Object, ObjectId>();
	protected Map<ObjectId, Object> depickled = new HashMap<ObjectId, Object>();

	public DB db;

	public MongoORM(DB d) {
		db = d;
	}

	public <T> List<T> loadAll(Class<T> clazz) {
		DBCollection coll = getCollection(clazz);
		if (coll == null)
			return null;

		List<T> l = new ArrayList<T>();
		DBCursor curr = coll.find();
		while (curr.hasNext()) {
			DBObject curr_o = curr.next();
			if (curr_o.get("row") == null) {
				T temp = (T) getObject(clazz, curr_o);
				l.add(temp);
			}
		}
		return l;

	}

	@SuppressWarnings("unchecked")
	private <T> T getObject(Class<T> clazz, DBObject curr_o) {
		ObjectId curr_id = (ObjectId) curr_o.get("_id");
		if (depickled.get(curr_id) != null) {
			return (T) depickled.get(curr_id);
		}
		T temp = null;
		try {
			temp = clazz.newInstance();
		} catch (InstantiationException e1) {
			e1.printStackTrace();
		} catch (IllegalAccessException e1) {
			e1.printStackTrace();
		}
		depickled.put(curr_id, temp);

		Field[] fields = getAllFields(clazz);
		for (Field f : fields) {
			Class<? extends Object> fieldType = f.getType();

			String fieldName = getFieldName(f);
			if (fieldName == null)
				continue;

			Object value = curr_o.get(fieldName);
			if (value == null)
				continue;

			if (value instanceof DBObject) {
				if (fieldType == Map.class) {
					DBObject mapEntry = (DBObject) value;
					DBObject mapMetadata = (DBObject) mapEntry.get("metadata");
					String mapTypeName = (String) mapMetadata.get("type");

					Class<? extends Object> mapType = null;
					try {
						mapType = Class.forName(mapTypeName);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
					Map<Object, Object> map = null;
					try {
						map = (Map<Object, Object>) mapType.newInstance();
					} catch (InstantiationException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}

					BasicDBList ids = (BasicDBList) mapEntry.get("data");
					for (Object elem_o : ids) {
						DBObject elem = (DBObject) elem_o;

						String mapKeyTypeName = (String) elem.get("keyType");
						Class<? extends Object> mapKeyType = null;
						try {
							mapKeyType = Class.forName(mapKeyTypeName);
						} catch (ClassNotFoundException e1) {
							e1.printStackTrace();
						}
						if (mapKeyType == null) {
							continue;
						}
						DBCollection keyColl = getCollection(mapKeyType);

						String mapValTypeName = (String) elem.get("valueType");
						Class<? extends Object> mapValType = null;
						try {
							mapValType = Class.forName(mapValTypeName);
						} catch (ClassNotFoundException e1) {
							e1.printStackTrace();
						}
						if (mapValType == null) {
							continue;
						}
						DBCollection valColl = getCollection(mapValType);

						Object keyItemId = elem.get("key");
						Object valItemId = elem.get("value");

						Object keyObj = null;
						Object valObj = null;

						if (getCollection(mapKeyType) != null) {
							DBObject query = new BasicDBObject();
							query.put("_id", keyItemId);
							DBObject keyDBObj = keyColl.findOne(query);
							keyObj = getObject(mapKeyType, keyDBObj);
						} else {
							keyObj = keyItemId;
						}

						if (getCollection(mapValType) != null) {
							DBObject query = new BasicDBObject();
							query.put("_id", valItemId);
							DBObject valDBObj = valColl.findOne(query);
							valObj = getObject(mapValType, valDBObj);
						} else {
							valObj = keyItemId;
						}

						try {
							map.put(keyObj, valObj);
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						}
					}
					try {
						f.set(temp, map);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				} else if (fieldType == List.class || fieldType == Set.class || fieldType == Queue.class) {
					DBObject collEntry = (DBObject) value;
					DBObject collMetadata = (DBObject) collEntry.get("metadata");
					String collTypeName = (String) collMetadata.get("type");

					Class<? extends Object> collType = null;
					try {
						collType = Class.forName(collTypeName);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
					Collection<Object> lst = null;
					try {
						lst = (Collection<Object>) collType.newInstance();
					} catch (InstantiationException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}

					BasicDBList ids = (BasicDBList) collEntry.get("data");
					for (Object elem_o : ids) {
						if (elem_o instanceof DBObject) {
							DBObject elem = (DBObject) elem_o;
							String className = (String) elem.get("type");

							Class<? extends Object> elemType = null;
							try {
								elemType = Class.forName(className);
							} catch (ClassNotFoundException e1) {
								e1.printStackTrace();
							}
							if (elemType == null) {
								continue;
							}
							DBCollection coll = getCollection(elemType);

							ObjectId item_id = (ObjectId) elem.get("item_id");
							DBObject query = new BasicDBObject();
							query.put("_id", item_id);
							DBObject linked_obj = coll.findOne(query);
							try {
								lst.add(getObject(elemType, linked_obj));
							} catch (IllegalArgumentException e) {
								e.printStackTrace();
							}
						} else {
							lst.add(elem_o);
						}
					}
					try {
						f.set(temp, lst);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				} else {
					DBObject entry = (DBObject) value;
					String className = (String) entry.get("type");

					Class<? extends Object> item_type = null;
					try {
						item_type = Class.forName(className);
					} catch (ClassNotFoundException e1) {
						e1.printStackTrace();
					}
					if (item_type == null) {
						continue;
					}
					DBCollection coll = getCollection(item_type);

					ObjectId item_id = (ObjectId) entry.get("item_id");
					DBObject query = new BasicDBObject();
					query.put("_id", item_id);
					DBObject linked_obj = coll.findOne(query);
					try {
						f.set(temp, getObject(item_type, linked_obj));
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			} else {
				try {
					f.set(temp, value);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return temp;
	}

	public void save(Object o) {
		saveObj(o);
	}

	private ObjectId saveObj(Object o) {
		Class<? extends Object> clazz = o.getClass();

		if (pickled.get(o) != null) {
			return pickled.get(o);
		}

		DBCollection coll = getCollection(clazz);
		if (coll == null)
			return null;

		if (coll.count() == 0) {
			DBObject metadata = new BasicDBObject();
			metadata.put("row", "metadata");
			metadata.put("type", clazz.getName());
			coll.insert(metadata);
		}

		BasicDBObject placeholder = new BasicDBObject("placeholder", true);
		placeholder.put("placeholder", true);
		coll.insert(placeholder);

		ObjectId id = (ObjectId) placeholder.get("_id");
		pickled.put(o, id);

		BasicDBObject row = new BasicDBObject();

		Field[] fields = getAllFields(clazz);

		for (Field f : fields) {
			String fieldName = getFieldName(f);
			if (fieldName == null)
				continue;

			Object value = null;
			try {
				value = f.get(o);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			if (value == null)
				continue;

			if (value instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<Object, Object> map = (Map<Object, Object>) value;
				Class<? extends Object> mapType = map.getClass();
				if (!map.isEmpty()) {
					Iterator<Object> iter = map.keySet().iterator();
					Object mapKey = iter.next();
					Object mapVal = map.get(mapKey);
					DBObject entry = new BasicDBObject();

					DBObject metadata = new BasicDBObject();

					metadata.put("type", mapType.getName());

					BasicDBList ids = new BasicDBList();
					DBObject elem = null;
					iter = map.keySet().iterator();
					while (iter.hasNext()) {
						mapKey = iter.next();
						mapVal = map.get(mapKey);
						elem = new BasicDBObject();
						ObjectId thisid = saveObj(mapKey);
						if (thisid == null)
							elem.put("key", mapKey);
						else
							elem.put("key", thisid);

						thisid = saveObj(mapVal);
						if (thisid == null)
							elem.put("value", mapVal);
						else
							elem.put("value", thisid);

						elem.put("keyType", mapKey.getClass().getName());
						elem.put("valueType", mapVal.getClass().getName());
						ids.add(elem);
					}
					entry.put("metadata", metadata);
					entry.put("data", ids);
					row.put(fieldName, entry);
				}
			} else if (value instanceof Collection) {
				@SuppressWarnings("unchecked")
				Collection<Object> collection = (Collection<Object>) value;
				Class<? extends Object> collType = collection.getClass();
				if (collection.size() > 0) {
					DBObject entry = new BasicDBObject();
					DBObject metadata = new BasicDBObject();

					metadata.put("type", collType.getName());
					BasicDBList ids = new BasicDBList();
					DBObject elem = null;
					for (Object list_o : collection) {
						if (getCollection(list_o.getClass()) != null) {
							elem = new BasicDBObject();
							elem.put("item_id", saveObj(list_o));
							elem.put("type", list_o.getClass().getName());
							ids.add(elem);
						} else {
							ids.add(list_o);
						}
					}
					entry.put("metadata", metadata);
					entry.put("data", ids);
					row.put(fieldName, entry);

				}
			} else if (value instanceof Object && getCollection(value.getClass()) != null) {
				DBObject item = new BasicDBObject();
				item.put("item_id", saveObj(value));
				item.put("type", value.getClass().getName());
				row.put(fieldName, item);
			} else {
				row.put(fieldName, value);
			}
		}
		coll.update(placeholder, row);
		return id;
	}

	private <T> DBCollection getCollection(Class<T> clazz) {
		Annotation anno = clazz.getAnnotation(MongoCollection.class);
		if (anno == null)
			return null;

		String collection = null;
		try {
			collection = (String) MongoCollection.class.getMethod("value").invoke(anno);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		if (collection.equals(""))
			collection = clazz.getName();
		return db.getCollection(collection);
	}

	private static String getFieldName(Field f) {
		Annotation anno = f.getAnnotation(MongoField.class);
		if (anno == null)
			return null;

		String fieldName = null;
		try {
			fieldName = (String) MongoField.class.getMethod("value").invoke(anno);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		if (fieldName.equals(""))
			fieldName = f.getName();

		return fieldName;
	}

	private static Field[] getAllFields(Class<?> clazz) {
		List<Field> fields = new ArrayList<Field>();
		fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
		if (clazz.getSuperclass() != null) {
			fields.addAll(Arrays.asList(getAllFields(clazz.getSuperclass())));
		}
		return fields.toArray(new Field[] {});
	}
}