/*
** CS414 nameserver.c
** Serves as a registry for streaming users
**  to connect to eachother
** Adapted from Beej's Guide to Network Programming
*/

/* Connection info:
** Client Sends this
**   Packet 1: 16 byte string for the username that is registering or being queried
**   Packet 2: 1 byte string for the type of connection
**               'a' == add the username from packet one
**               'q' == query for the username from packet one's IP
**               'r' == remove the username from packet one
** Server responds with this:
**   Packet1: 48 byte string that contains the IP address that was requested from a query
**                 otherwise all 48 bytes will be 0x00
**                 Similarly, if the username was not found in the nameserver, they will be 0x00
**/

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <arpa/inet.h>
#include <sys/wait.h>
#include <signal.h>
#include <pthread.h>

#define NSREGISTER 0
#define NSQUERY 1
#define NSUPDATE 2
#define NSREMOVE 3
#define NAMESIZE 16

char* ns_nameserverip;
char* ns_nameserverport;
int firstConnection;

typedef enum sresult {
	SNONE,
	SPART,
	SFULL
} sresult;

typedef struct ns_entry {
	char username[16];
	char resource[16];
	char ip[INET6_ADDRSTRLEN];
	int timeout;
	struct ns_entry* next;
} ns_entry;

typedef struct ns_list {
	ns_entry* head;
	int user_count;
	pthread_mutex_t* lock;
} ns_list;

typedef struct search_info{
	ns_entry* target;
	ns_entry* prior;
} search_info;

#define PORT "3825"  // the port users will be connecting to

#define BACKLOG 10	 // how many pending connections queue will hold

ns_list* userlist;

//Searches the ns_list for a username and resource, returns varying levels of success in result;
int list_search(ns_list* list, char* username, char* resource, search_info* result){
	//Trying to search empty list
	if(list->head == NULL)
		return 0;

	result->target = NULL;
	result->prior = NULL;
	sresult match = SNONE;

	ns_entry* current = NULL;
	ns_entry* prior = NULL;
	
	current = list->head;
	do {
		if(0 == strcmp(username, current->username)){
			result->target = current;
			result->prior = prior;
			if(0 == strlen(resource) || 0 == strcmp(resource, current->resource)){
				return SFULL; //FULL MATCH
			}
			else {
				match = SPART; //PARTIAL MATCH
			}
		}
		prior = current;
		current = current->next;
	} while(current != NULL);

	return match;
}

void list_add(ns_list* list, char* username, char* ip){
	//Check to see if the exact entry is already in the list
	char* subptr;
	char* newip;

	search_info result;
	sresult match;
	match = list_search(list, username, "", &result);
	if(match == SFULL)
		return;

	ns_entry* newuser = malloc(sizeof(ns_entry));
	strncpy(newuser->username, username, 16);
	strncpy(newuser->resource, "", 16);

	newip = ip;
	while(ip != NULL) {
		ip = strstr(ip,":");
		if(ip == NULL)
			break;
		ip += 1;
		newip = ip;
	}

	strncpy(newuser->ip, newip, INET6_ADDRSTRLEN);
	newuser->next = list->head;
	list->head = newuser;
}

//If resource is a null string, then remove all instances of username.
void list_remove(ns_list* list, char* username, char* resource){
	search_info result;
	sresult match;
	int has_resource = strlen(resource);
	int removed_match = 0;

	while(1){
		match = list_search(list, username, resource, &result);
		switch (match){
			case SNONE:
				return;
			case SPART:
				if (has_resource){
					if (!removed_match)
						break;
					else
						return;
				}
			case SFULL:
				removed_match = 1;
				if(result.prior == NULL)
					list->head = result.target->next;
				else
					result.prior->next = result.target->next;
				free(result.target);
		}
	}
}

void sigchld_handler(int s)
{
	while(waitpid(-1, NULL, WNOHANG) > 0);
}

// get sockaddr, IPv4 or IPv6:
void *get_in_addr(struct sockaddr *sa)
{
	if (sa->sa_family == AF_INET) {
		return &(((struct sockaddr_in*)sa)->sin_addr);
	}

	return &(((struct sockaddr_in6*)sa)->sin6_addr);
}

void* newUser(void* new_fd);

typedef struct t_data{
	int fd;
	char s[INET6_ADDRSTRLEN];
} t_data;

int main(int argc, char* argv[])
{
	int sockfd, new_fd;  // listen on sock_fd, new connection on new_fd
	struct addrinfo hints, *servinfo, *p;
	struct sockaddr_storage their_addr; // connector's address information
	socklen_t sin_size;
	struct sigaction sa;
	int yes=1;
	firstConnection = 0;
	char s[INET6_ADDRSTRLEN];
	int rv;

	pthread_t pthr_con;
	char message[100];
	t_data* thread_data;

	memset(&hints, 0, sizeof hints);
	hints.ai_family = AF_UNSPEC;
	hints.ai_socktype = SOCK_STREAM;
	hints.ai_flags = AI_PASSIVE; // use my IP

	if ((rv = getaddrinfo(NULL, PORT, &hints, &servinfo)) != 0) {
		fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(rv));
		return 1;
	}

	// loop through all the results and bind to the first we can
	for(p = servinfo; p != NULL; p = p->ai_next) {
		if ((sockfd = socket(p->ai_family, p->ai_socktype,
				p->ai_protocol)) == -1) {
			perror("server: socket");
			continue;
		}

		if (setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &yes,
				sizeof(int)) == -1) {
			perror("setsockopt");
			exit(1);
		}

		if (bind(sockfd, p->ai_addr, p->ai_addrlen) == -1) {
			close(sockfd);
			perror("server: bind");
			continue;
		}

		break;
	}

	if (p == NULL)  {
		fprintf(stderr, "server: failed to bind\n");
		return 2;
	}

	freeaddrinfo(servinfo); // all done with this structure

	if (listen(sockfd, BACKLOG) == -1) {
		perror("listen");
		exit(1);
	}

	sa.sa_handler = sigchld_handler; // reap all dead processes
	sigemptyset(&sa.sa_mask);
	sa.sa_flags = SA_RESTART;
	if (sigaction(SIGCHLD, &sa, NULL) == -1) {
		perror("sigaction");
		exit(1);
	}

	//Initialize the linked list of users
	userlist = malloc(sizeof(ns_list));
	userlist->head = NULL;
	userlist->user_count = 0;
	userlist->lock = malloc(sizeof(pthread_mutex_t));
	pthread_mutex_init(userlist->lock, NULL);


	printf("server: waiting for connections...\n");

	while(1) {  // main accept() loop
		sin_size = sizeof their_addr;
		new_fd = accept(sockfd, (struct sockaddr *)&their_addr, &sin_size);
		if (new_fd == -1) {
			perror("accept");
			continue;
		}

		inet_ntop(their_addr.ss_family,
			get_in_addr((struct sockaddr *)&their_addr),
			s, sizeof s);
		printf("server: got connection from %s\n", s);

		//Set up a thread_data struct.
		thread_data = (t_data*) malloc(sizeof(t_data));
		if (thread_data == NULL)
		{
			fprintf(stderr,"Malloc failed\n");
			exit(1);
		}
		thread_data->fd = new_fd;
		memcpy(thread_data->s, s, sizeof(s));

		//Spawn a new thread to send the message to the client
		if (pthread_create(&pthr_con, NULL, newUser, (void*) thread_data))
		{
			close(new_fd);  //pthread_create failed
			free(thread_data);
		}
	}
	return 0;
}

void* newUser(void* thread_data)
{
	t_data* t_d = (t_data*)thread_data;
	int fd = t_d->fd;
//	uint32_t size = htonl(t_d->bytes);

	char name[16];
	char type[16];
	char ip[48];
	memset(ip,0x7A7A,48);
	//new_c.type = 0xA5A5;
	//new_c.usr_num = 0xA5A5;

	printf("Waiting for name from android\n");
	if (recv(fd, name, 16, 0) == -1)
	{
		perror("recv");
	}

	printf("Waiting for type from android\n");
	if (recv(fd, type, 16, 0) == -1)
	{
		perror("recv");
	}

	int numtype = 10;

	printf("name: %s\n",name);
	printf("type: %s\n",type);

	if(firstConnection) {
		ip[0] = 'x';
		if (send(fd, &ip, 48, 0) == -1)
		{
			perror("send");
		}
		close(fd);
		free(thread_data);
		firstConnection = 0;
		return NULL;
	}


	if(type[0] == 'a')
		numtype = NSREGISTER;
	else if (type[0] == 'q')
		numtype = NSQUERY;
	else if (type[0] == 'r')
		numtype = NSREMOVE;

	printf("+++++++++++++++\n");
	sresult result;
	search_info sinfo;
	
	switch(numtype) {
		case NSREGISTER:
			printf("Connection Type: REGISTER\n");
			printf("Name being registered: %s\n",name);
			printf("IP being registered: %s\n",t_d->s);
			list_add(userlist,name, t_d->s);
			//Enter the users info into the linked list
			break;
		case NSQUERY:
			printf("Connection Type: QUERY\n");
			//Give the user the IP of the name they requested;
			result = list_search(userlist,name,"",&sinfo);
			printf("Name being queried: %s\n",name);
			switch (result) {
				case SNONE:
					printf("Query failed: User not found\n");
					break;
				case SFULL:
					printf("Full query match\n");
					printf("User: %s\n", sinfo.target->username);
					printf("IP: %s\n",   sinfo.target->ip);
					strncpy(ip,sinfo.target->ip,48);
					break;
				default:
					break;
				
			}
			break;
		case NSREMOVE:
			printf("Connection Type: REMOVE\n");
			//Remove the user from the userlist
			printf("Name being removed: %s\n",name);
			list_remove(userlist,name,"");
			break;
		default:
			printf("Connection Type: UNKNOWN\n");
			break;
	}		
	printf("Replying to IP address %s\n", t_d->s);
	printf("---------------\n");

	if (send(fd, &ip, 48, 0) == -1)
	{
		perror("send");
	}

	close(fd);
	free(thread_data);
	return NULL;
}
