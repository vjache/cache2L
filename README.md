## Two Level Cache Test Task

### Problem
Create a configurable two-level cache (for caching Objects).  
Level 1 is memory, level 2 is filesystem.  
Config params should let one specify the cache strategies and max sizes of level 1 and 2.

### Solution

This very simplistic implementation of a multilayer cache is designed as 
a group of specific caches which can be chained like a russian "matryoshka".
The framework of this solution consists of two abstract classes:
 * Cache - interface
 * CacheLayer - abstract class for chained caches
There are also specific caches:
 * MemCacheMFU - in-memory cache with Most Frequently Used retain strategy
 * MemCacheMRU - in-memory cache with Most Recently Used retain strategy
 * FileCache   - persistent cache based on files
 * MMFileCache - persistent cache based on memory mapped file
 * ValueComputer - cache which is not a real cache but a ine which 
   computes the values. It may be used to request e.g. DB for real 
   expensive query. This cache intended to be appended as a third or 
   fourth layer.
 * DummyCache - it is a cache which does not cache it is intended to be 
   a very last element in a cache layers.
 
Please see unit tests to know how to construct multilayered caches e.g. at:
 * CacheTest.twoLayerCacheTest()
 * CacheTest.twoLayerCacheTest2()

and also docs in each class.