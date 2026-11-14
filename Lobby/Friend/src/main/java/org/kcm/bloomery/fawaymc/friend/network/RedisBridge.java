package org.kcm.bloomery.fawaymc.friend.network;

import java.util.Objects;
import java.util.function.BiConsumer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

public class RedisBridge {
   private final JedisPool pool;
   private final BiConsumer<String, String> onMessage;
   private Thread subscriber;
   private volatile boolean closed = false;

   public RedisBridge(String host, int port, String password, int db, int timeout, BiConsumer<String, String> onMessage) {
      this.onMessage = onMessage;
      JedisPoolConfig cfg = new JedisPoolConfig();
      cfg.setMaxTotal(8);
      cfg.setMaxIdle(4);
      cfg.setMinIdle(1);
      cfg.setTestOnBorrow(true);
      this.pool = new JedisPool(cfg, host, port, timeout, password != null && !password.isEmpty() ? password : null, db);
   }

   public boolean ping() {
      try {
         Jedis j = this.pool.getResource();

         boolean var2;
         try {
            var2 = "PONG".equals(j.ping());
         } catch (Throwable var5) {
            if (j != null) {
               try {
                  j.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }
            }

            throw var5;
         }

         if (j != null) {
            j.close();
         }

         return var2;
      } catch (Throwable var6) {
         return false;
      }
   }

   public void publish(String channel, String message) {
      if (!this.closed) {
         try {
            Jedis j = this.pool.getResource();

            try {
               j.publish(channel, message);
            } catch (Throwable var7) {
               if (j != null) {
                  try {
                     j.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (j != null) {
               j.close();
            }
         } catch (Throwable var8) {
         }

      }
   }

   public void hset(String key, String field, String value) {
      if (!this.closed) {
         try {
            Jedis j = this.pool.getResource();

            try {
               j.hset(key, field, value);
            } catch (Throwable var8) {
               if (j != null) {
                  try {
                     j.close();
                  } catch (Throwable var7) {
                     var8.addSuppressed(var7);
                  }
               }

               throw var8;
            }

            if (j != null) {
               j.close();
            }
         } catch (Throwable var9) {
         }

      }
   }

   public String hget(String key, String field) {
      if (this.closed) {
         return null;
      } else {
         try {
            Jedis j = this.pool.getResource();

            String var4;
            try {
               var4 = j.hget(key, field);
            } catch (Throwable var7) {
               if (j != null) {
                  try {
                     j.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (j != null) {
               j.close();
            }

            return var4;
         } catch (Throwable var8) {
            return null;
         }
      }
   }

   public void hdel(String key, String... fields) {
      if (!this.closed) {
         try {
            Jedis j = this.pool.getResource();

            try {
               j.hdel(key, fields);
            } catch (Throwable var7) {
               if (j != null) {
                  try {
                     j.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (j != null) {
               j.close();
            }
         } catch (Throwable var8) {
         }

      }
   }

   public void startSubscriber(String channel) {
      if (!this.closed) {
         this.subscriber = new Thread(() -> {
            try {
               Jedis j = this.pool.getResource();

               try {
                  j.subscribe(new JedisPubSub() {
                     {
                        Objects.requireNonNull(RedisBridge.this);
                     }

                     public void onMessage(String ch, String msg) {
                        try {
                           if (RedisBridge.this.onMessage != null) {
                              RedisBridge.this.onMessage.accept(ch, msg);
                           }
                        } catch (Throwable var4) {
                        }

                     }
                  }, new String[]{channel});
               } catch (Throwable var6) {
                  if (j != null) {
                     try {
                        j.close();
                     } catch (Throwable x2) {
                        var6.addSuppressed(x2);
                     }
                  }

                  throw var6;
               }

               if (j != null) {
                  j.close();
               }
            } catch (Throwable var7) {
            }

         }, "Friend-Redis-Sub");
         this.subscriber.setDaemon(true);
         this.subscriber.start();
      }
   }

   public void close() {
      this.closed = true;
      if (this.subscriber != null) {
         try {
            this.subscriber.interrupt();
         } catch (Throwable var3) {
         }
      }

      try {
         if (this.pool != null && !this.pool.isClosed()) {
            this.pool.close();
         }
      } catch (Throwable var2) {
      }

   }
}
